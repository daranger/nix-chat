package uz.nixchat.common.storage;

import uz.nixchat.common.model.message.Message;

import java.util.List;

/**
 * Where messages are kept. The client uses a file-based implementation,
 * tests use {@link InMemoryMessageStorage}; the server will get a PostgreSQL one.
 */
public interface MessageStorage {

    void save(Message message);

    /** Messages of one chat, oldest first. */
    List<Message> findByChat(String chatId);

    /** The last {@code limit} messages of a chat, oldest first. */
    default List<Message> findLast(String chatId, int limit) {
        List<Message> all = findByChat(chatId);
        int from = Math.max(0, all.size() - limit);
        return all.subList(from, all.size());
    }

    int count();
}
