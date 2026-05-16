package com.skillmatch.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Component;

import org.bson.Document;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class MongoIndexConfig implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Creating MongoDB indexes...");

        // users
        mongoTemplate.indexOps("users")
                .ensureIndex(new Index("email", Sort.Direction.ASC).unique());

        // companies
        mongoTemplate.indexOps("companies")
                .ensureIndex(new Index("userId", Sort.Direction.ASC));
        mongoTemplate.indexOps("companies")
                .ensureIndex(new CompoundIndexDefinition(new Document("industry", 1).append("active", 1)));

        // jobs
        mongoTemplate.indexOps("jobs")
                .ensureIndex(new Index("companyId", Sort.Direction.ASC));
        mongoTemplate.indexOps("jobs")
                .ensureIndex(new CompoundIndexDefinition(new Document("status", 1).append("active", 1)));
        mongoTemplate.indexOps("jobs")
                .ensureIndex(TextIndexDefinition.builder().onField("title").build());

        // applications
        mongoTemplate.indexOps("applications")
                .ensureIndex(new Index("userId", Sort.Direction.ASC));
        mongoTemplate.indexOps("applications")
                .ensureIndex(new Index("jobId", Sort.Direction.ASC));
        mongoTemplate.indexOps("applications")
                .ensureIndex(new CompoundIndexDefinition(new Document("userId", 1).append("jobId", 1)).unique());

        // messages
        mongoTemplate.indexOps("messages")
                .ensureIndex(new CompoundIndexDefinition(new Document("senderId", 1).append("receiverId", 1)));

        // connections
        mongoTemplate.indexOps("connections")
                .ensureIndex(new Index("userId", Sort.Direction.ASC));
        mongoTemplate.indexOps("connections")
                .ensureIndex(new Index("connectedUserId", Sort.Direction.ASC));
        mongoTemplate.indexOps("connections")
                .ensureIndex(new CompoundIndexDefinition(new Document("userId", 1).append("connectedUserId", 1)).unique());

        // saved_jobs
        mongoTemplate.indexOps("saved_jobs")
                .ensureIndex(new Index("userId", Sort.Direction.ASC));
        mongoTemplate.indexOps("saved_jobs")
                .ensureIndex(new CompoundIndexDefinition(new Document("userId", 1).append("jobId", 1)).unique());

        // notifications
        mongoTemplate.indexOps("notifications")
                .ensureIndex(new CompoundIndexDefinition(new Document("userId", 1).append("isRead", 1)));

        log.info("MongoDB indexes created successfully");
    }
}
