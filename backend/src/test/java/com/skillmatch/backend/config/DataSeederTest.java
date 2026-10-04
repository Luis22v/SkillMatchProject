package com.skillmatch.backend.config;

import com.skillmatch.backend.model.Company;
import com.skillmatch.backend.model.Job;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.ApplicationRepository;
import com.skillmatch.backend.repository.CompanyRepository;
import com.skillmatch.backend.repository.JobRepository;
import com.skillmatch.backend.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    private static final String ENCODED = "$2a$10$encodedPasswordHash";

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void run_emptyDatabase_encodesPasswordOnceAndReusesHash() {
        // El seeder reutiliza y vacía su lista de lote tras cada saveAll, así que se copian los elementos guardados.
        List<User> savedUsers = new ArrayList<>();
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(DataSeeder.DEFAULT_PASSWORD)).thenReturn(ENCODED);
        // createApplications usa los IDs devueltos por saveAll como clave única: sin IDs, el seed no termina.
        when(userRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            List<User> batch = assignIds(inv.getArgument(0), User::setId);
            savedUsers.addAll(batch);
            return batch;
        });
        when(companyRepository.saveAll(anyIterable())).thenAnswer(inv -> assignIds(inv.getArgument(0), Company::setId));
        when(jobRepository.saveAll(anyIterable())).thenAnswer(inv -> assignIds(inv.getArgument(0), Job::setId));

        dataSeeder.run();

        verify(passwordEncoder, times(1)).encode(DataSeeder.DEFAULT_PASSWORD);
        assertThat(savedUsers)
                .hasSize(4000) // 3000 usuarios + 1000 cuentas de empresa
                .allSatisfy(user -> assertThat(user.getPassword()).isEqualTo(ENCODED));
        verify(applicationRepository, atLeastOnce()).saveAll(anyIterable());
    }

    @Test
    void run_databaseAlreadySeeded_skipsSeed() {
        when(userRepository.count()).thenReturn(10L);

        dataSeeder.run();

        verifyNoInteractions(passwordEncoder, companyRepository, jobRepository, applicationRepository);
        verify(userRepository, never()).saveAll(anyIterable());
    }

    private static <T> List<T> assignIds(List<T> entities, BiConsumer<T, String> idSetter) {
        entities.forEach(entity -> idSetter.accept(entity, ObjectId.get().toString()));
        return entities;
    }
}
