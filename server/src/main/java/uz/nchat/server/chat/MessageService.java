package uz.nchat.server.chat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepo;
    private final ConversationMemberRepository memberRepo;

    public MessageService(MessageRepository messageRepo, ConversationMemberRepository memberRepo) {
        this.messageRepo = messageRepo;
        this.memberRepo = memberRepo;
    }

    // Time complexity: O(c + log n), where c is the message length and n is the number of rows.
    // The membership check and the insert each use an index, and the blank check reads the text once.
    // Space complexity: O(1). We create one Message object and one DTO. Both point to the
    // same content string, so we do not copy the text.
    public MessageDTO send(Long conversationId, Long senderId, String content) {
        // One indexed lookup, see checkMember: O(log n).
        checkMember(conversationId, senderId);

        // isBlank() scans the text until it finds a non-space character. In the worst case
        // it reads the whole message once, so it is O(c).
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is empty");
        }

        // Saving inserts one row and updates the index, which is O(log n).
        // Writing the text itself costs O(c). Converting to a DTO is O(1).
        return toDTO(messageRepo.save(new Message(conversationId, senderId, content)));
    }

    // Time complexity: O(log n + M), where M is the number of messages in the conversation.
    // We do one membership check, then read all M messages of the chat.
    // Space complexity: O(M). We hold a list of M messages and then a list of M DTOs.
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(Long conversationId, Long userId) {
        // One indexed lookup: O(log n).
        checkMember(conversationId, userId);

        // The database finds the first message of the conversation through the index in
        // O(log n) and then reads the M messages in order: O(log n + M).
        // This assumes an index on (conversation_id, id), so the rows already come out
        // sorted by id and no extra sort is needed.
        // Then map() converts each message to a DTO in one pass, which is O(M).
        return messageRepo.findByConversationIdOrderByIdAsc(conversationId)
            .stream().map(this::toDTO).toList();
    }

    // Time complexity: O(log n). existsBy... is one indexed lookup on (conversation_id, user_id).
    // Space complexity: O(1). We only keep a boolean result.
    private void checkMember(Long conversationId, Long userId) {
        if (!memberRepo.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this conversation");
        }
    }

    // Time complexity: O(1). We read five fields and create one object.
    // Space complexity: O(1). We create a single DTO.
    private MessageDTO toDTO(Message m) {
        return new MessageDTO(m.getId(), m.getConversationId(), m.getSenderId(),
            m.getContent(), m.getSentAt());
    }

    /*
     * HOW WE DERIVED THE TIME COMPLEXITY
     *
     * Let n be the number of rows in the database table, M the number of messages in one
     * conversation, and c the length of one message.
     *
     * checkMember():
     * It calls existsByConversationIdAndUserId, which is a single search on indexed columns.
     * A B-tree search takes O(log n), so this method is O(log n).
     *
     * send():
     * We call checkMember (O(log n)), then isBlank(), which reads the message once in the
     * worst case (O(c)), then save(), which inserts one row and updates the index (O(log n)).
     * Adding them gives O(log n + c + log n), which simplifies to O(c + log n).
     *
     * getMessages():
     * We call checkMember (O(log n)). Then the query finds the start of the conversation in
     * the index (O(log n)) and reads M rows (O(M)). Finally map() goes over M messages once
     * (O(M)). The total is O(log n + M).
     *
     * toDTO():
     * It reads five fields and builds one object, so it is O(1). It is called once in send()
     * and M times in getMessages(), which we already counted above.
     *
     * Assumption: the results are only true if the columns in the queries are indexed.
     * If there is no index on conversation_id, the database scans the whole table (O(n)).
     * If there is an index only on conversation_id and not on (conversation_id, id), the
     * database must sort the M messages itself, and getMessages() becomes O(log n + M log M).
     *
     * HOW WE DERIVED THE SPACE COMPLEXITY
     *
     * We counted the extra memory each method creates while it runs, not the database storage.
     *
     * send():
     * We create one Message and one DTO. Both reuse the same content string, so no text is
     * copied and the extra space is O(1).
     *
     * getMessages():
     * We keep a list of M messages loaded from the database and then a list of M DTOs.
     * That is 2 * O(M), which simplifies to O(M) because constants are dropped.
     *
     * checkMember() and toDTO():
     * They only keep a boolean or create one object, so both are O(1).
     */
}
