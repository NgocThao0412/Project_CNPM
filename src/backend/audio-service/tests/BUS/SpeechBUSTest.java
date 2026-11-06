
package BUS;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import DAO.SpeechDAO;

class SpeechBUSTest {

    private SpeechDAO dao;
    private GeminiSpeechBUS speech;
    private AudioBUS audio;
    private PlatformTransactionManager transactionManager;
    private SpeechBUS service;

    @BeforeEach
    void setUp() {
        dao = mock(SpeechDAO.class);
        speech = mock(GeminiSpeechBUS.class);
        audio = mock(AudioBUS.class);
        transactionManager = mock(PlatformTransactionManager.class);

        service = new SpeechBUS(
                dao, speech, audio, transactionManager
        );
    }

    @Test
    void invalidContentIdReturns400() {
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.generate(0)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(dao, speech, audio);
    }

    @Test
    void missingNarrationReturns404() {
        when(dao.find(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.generate(999L)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verifyNoInteractions(speech, audio);
    }

    @Test
    void inactiveLanguageReturns400() {
        when(dao.find(10L)).thenReturn(
                Optional.of(
                        new SpeechDAO.Source(
                                10L, "Xin chào", "vi", false
                        )
                )
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.generate(10L)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(speech, audio);
    }

    @Test
    void changedContentMustNotSaveAudio() {
        when(dao.find(10L)).thenReturn(
                Optional.of(
                        new SpeechDAO.Source(
                                10L, "Nội dung cũ", "vi", true
                        )
                )
        );

        when(speech.generateMp3("Nội dung cũ", "vi"))
                .thenReturn(new byte[] {1, 2, 3});

        when(transactionManager.getTransaction(any()))
                .thenReturn(mock(TransactionStatus.class));

        when(dao.lock(10L)).thenReturn(
                Optional.of(
                        new SpeechDAO.Source(
                                10L, "Nội dung mới", "vi", true
                        )
                )
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.generate(10L)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(audio);
    }
}
