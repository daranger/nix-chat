package uz.nchat.common.storage;

import uz.nchat.common.model.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps messages in memory only; everything is lost on restart.
 */
public class InMemoryMessageStorage implements MessageStorage {

    private final List<Message> messages = new ArrayList<>();

    @Override
    public synchronized void save(Message message) {
        messages.add(message);
    }

    @Override
    public synchronized List<Message> findByChat(String chatId) {
        List<Message> result = new ArrayList<>();
        for (Message message : messages) {
            if (message.getChatId().equals(chatId)) {
                result.add(message);
            }
        }
        return result;
    }

    @Override
    public synchronized int count() {
        return messages.size();
    }
}
