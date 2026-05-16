package com.skillmatch.backend.repository;

import com.skillmatch.backend.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByReceiverIdAndIsReadFalseAndDeletedByReceiverFalse(String receiverId);

    long countByReceiverIdAndIsReadFalseAndDeletedByReceiverFalse(String receiverId);

    long countBySenderIdAndReceiverIdAndIsReadFalseAndDeletedByReceiverFalse(String senderId, String receiverId);

    List<Message> findBySenderIdAndReceiverIdAndIsReadFalseAndDeletedByReceiverFalse(String senderId, String receiverId);
}
