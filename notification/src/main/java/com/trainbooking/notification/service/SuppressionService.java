package com.trainbooking.notification.service;

import com.trainbooking.notification.domain.entity.SuppressionEntry;
import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.SuppressionReason;
import com.trainbooking.notification.repository.SuppressionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SuppressionService {

    private final SuppressionRepository repo;

    public SuppressionService(SuppressionRepository repo) {
        this.repo = repo;
    }

    public boolean isSuppressed(Channel channel, String recipient) {
        return repo.existsByChannelAndRecipient(channel, recipient);
    }

    @Transactional
    public SuppressionEntry add(
            Channel channel,
            String recipient,
            SuppressionReason reason,
            String sourceProvider,
            String notes) {
        return repo.findByChannelAndRecipient(channel, recipient)
                .orElseGet(() -> {
                    SuppressionEntry entry = new SuppressionEntry();
                    entry.setChannel(channel);
                    entry.setRecipient(recipient);
                    entry.setReason(reason);
                    entry.setSourceProvider(sourceProvider);
                    entry.setNotes(notes);
                    return repo.save(entry);
                });
    }

    @Transactional
    public void remove(Long id) {
        repo.deleteById(id);
    }

    public List<SuppressionEntry> list() {
        return repo.findAll();
    }
}
