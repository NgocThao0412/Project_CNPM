package BUS;

import java.io.IOException;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import DAO.SpeechDAO;
import DTO.AudioDTO;

@Service
public class SpeechBUS {

    private final SpeechDAO dao;
    private final GeminiSpeechBUS speech;
    private final AudioBUS audio;
    private final TransactionTemplate transaction;

    public SpeechBUS(
            SpeechDAO dao,
            GeminiSpeechBUS speech,
            AudioBUS audio,
            PlatformTransactionManager transactionManager
    ) {
        this.dao = dao;
        this.speech = speech;
        this.audio = audio;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public AudioDTO generate(long contentId) {
        if (contentId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID bài thuyết minh phải lớn hơn 0"
            );
        }

        SpeechDAO.Source source = dao.find(contentId).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy bài thuyết minh"
                )
        );

        if (!source.languageActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngôn ngữ của bài đã ngừng hỗ trợ"
            );
        }

        // Gọi Gemini trước, không giữ khóa database trong lúc chờ.
        byte[] mp3 = speech.generateMp3(
                source.content(),
                source.languageCode()
        );

        AudioDTO saved = transaction.execute(status -> {
            SpeechDAO.Source current = dao.lock(contentId).orElseThrow(
                    () -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Bài thuyết minh không còn tồn tại"
                    )
            );

            if (!current.languageActive()
                    || !Objects.equals(source.content(), current.content())
                    || !Objects.equals(
                            source.languageCode(),
                            current.languageCode()
                    )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Bài hoặc ngôn ngữ đã thay đổi trong lúc tạo audio. "
                                + "Hãy tải lại bài rồi tạo lại."
                );
            }

            String filename = "tts-" + contentId
                    + "-" + source.languageCode() + ".mp3";

            try {
                // Dùng lại kiểm tra MP3 và lưu phiên bản của AudioBUS.
                return audio.uploadRaw(contentId, filename, mp3);
            } catch (IOException exception) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không lưu được file âm thanh",
                        exception
                );
            }
        });

        if (saved == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Chưa lưu được metadata âm thanh"
            );
        }

        return saved;
    }
}