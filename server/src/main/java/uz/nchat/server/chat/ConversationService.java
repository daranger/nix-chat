package uz.nchat.server.chat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepo;
    private final ConversationMemberRepository memberRepo;

    public ConversationService(ConversationRepository conversationRepo,
                               ConversationMemberRepository memberRepo) {
        this.conversationRepo = conversationRepo;
        this.memberRepo = memberRepo;
    }

    // Time complexity: O(log n). We do one indexed search, and if no chat exists
    // we do three inserts, each of which also costs O(log n).
    // returns the existing private chat if there is one
    public ConversationDTO getOrCreatePrivate(Long userId, Long otherUserId) {
        // findPrivate looks for a chat shared by both users. With indexes on the
        // user and conversation columns, the database finds it in O(log n).
        return toDTO(conversationRepo.findPrivate(userId, otherUserId).orElseGet(() -> {
            // Inserting a row also updates the table index, so each save is O(log n).
            // We do exactly three saves (the conversation and two members), which is a constant number.
            Conversation c = conversationRepo.save(new Conversation(false, null));
            memberRepo.save(new ConversationMember(c.getId(), userId));
            memberRepo.save(new ConversationMember(c.getId(), otherUserId));
            return c;
        }));
    }

    // Time complexity: O(m log n), where m is the number of member ids we receive.
    // We save one row per member, and each save costs O(log n).
    public ConversationDTO createGroup(String title, Long creatorId, List<Long> memberIds) {
        // One insert for the conversation and one for the creator: 2 * O(log n).
        Conversation c = conversationRepo.save(new Conversation(true, title));
        memberRepo.save(new ConversationMember(c.getId(), creatorId));

        memberIds.stream()
            // distinct() uses a hash set internally, so it passes over the m ids once: O(m).
            .distinct()
            // filter() checks each id one time with an O(1) comparison: O(m) in total.
            .filter(id -> !id.equals(creatorId))
            // In the worst case we save m members, and each save is O(log n): O(m log n).
            // This is the most expensive part of the method.
            .forEach(id -> memberRepo.save(new ConversationMember(c.getId(), id)));
        return toDTO(c); // O(1)
    }

    // Time complexity: O(k log n), where k is the number of conversations the user is in.
    // The main cost is loading k conversations by id.
    @Transactional(readOnly = true)
    public List<ConversationDTO> getUserConversations(Long userId) {
        // findByUserId uses the index on user id, which costs O(log n + k) to find and return
        // the k membership rows. Then map() passes over those k rows once: O(k).
        List<Long> ids = memberRepo.findByUserId(userId).stream()
            .map(ConversationMember::getConversationId)
            .toList();
        // findAllById looks up each of the k ids by primary key, and each lookup is O(log n),
        // so this is O(k log n). Converting the results to DTOs is one more pass: O(k).
        return conversationRepo.findAllById(ids).stream().map(this::toDTO).toList();
    }

    // Time complexity: O(1). We only read four fields and create one object.
    private ConversationDTO toDTO(Conversation c) {
        return new ConversationDTO(c.getId(), c.isGroupChat(), c.getTitle(), c.getCreatedAt());
    }

    /*
     * HOW WE DERIVED THE TIME COMPLEXITY
     *
     * Let n be the number of rows in the database table, m the number of member ids
     * passed to createGroup, and k the number of conversations one user belongs to.
     *
     * getOrCreatePrivate():
     * We first search for an existing private chat, which takes O(log n) because the
     * database uses an index. If none exists, we make three saves. Each save is O(log n)
     * and three is a constant, so the total is still O(log n).
     *
     * createGroup():
     * Two saves cost O(log n). Then distinct() and filter() each pass over m ids once,
     * which is O(m). Finally we save up to m members, and each save costs O(log n), so
     * this part is O(m log n). We add the parts: O(log n + m + m log n). The largest term
     * wins, so the result is O(m log n).
     *
     * getUserConversations():
     * We find the k membership rows by index in O(log n + k) and map them in O(k). Then
     * findAllById does k primary-key lookups at O(log n) each, which is O(k log n).
     * The largest term is O(k log n), so that is the final answer.
     *
     * toDTO():
     * It copies four values into a new object, so it is O(1). It is called once in the
     * first two methods and k times in the last, which is already counted above.
     *
     * Assumption: the O(log n) results are only true if the columns used in the queries
     * (user id, conversation id) are indexed. Without indexes, each search would scan the
     * whole table and cost O(n). Primary keys are always indexed.
     */
}
