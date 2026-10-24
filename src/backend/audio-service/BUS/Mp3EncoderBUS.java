package BUS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.stereotype.Service;

import de.sciss.jump3r.lowlevel.LameEncoder;

@Service
public class Mp3EncoderBUS {

    private static final int MAX_WAV_SIZE = 30 * 1024 * 1024;
    private static final int MAX_MP3_SIZE = 20 * 1024 * 1024;

    public byte[] encode(byte[] wav) throws IOException {
        if (wav == null || wav.length < 12) {
            throw new IOException("Dữ liệu WAV rỗng hoặc không hợp lệ");
        }

        if (wav.length > MAX_WAV_SIZE) {
            throw new IOException("Dữ liệu WAV vượt quá 30 MB");
        }

        boolean waveHeader =
                wav[0] == 'R' && wav[1] == 'I'
                && wav[2] == 'F' && wav[3] == 'F'
                && wav[8] == 'W' && wav[9] == 'A'
                && wav[10] == 'V' && wav[11] == 'E';

        if (!waveHeader) {
            throw new IOException("Dữ liệu không có định dạng WAV");
        }

        try (
                AudioInputStream input = AudioSystem.getAudioInputStream(
                        new ByteArrayInputStream(wav)
                );
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            AudioFormat format = input.getFormat();

            if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
                    || format.getSampleSizeInBits() != 16
                    || format.isBigEndian()
                    || (format.getChannels() != 1
                        && format.getChannels() != 2)) {
                throw new IOException(
                        "Chỉ hỗ trợ WAV PCM 16-bit little-endian, mono hoặc stereo"
                );
            }

            int channelMode = format.getChannels() == 1
                    ? LameEncoder.CHANNEL_MODE_MONO
                    : LameEncoder.CHANNEL_MODE_STEREO;

            LameEncoder encoder = new LameEncoder(
                    format,
                    96,
                    channelMode,
                    2,
                    false
            );

            try {
                byte[] pcmBuffer = new byte[encoder.getPCMBufferSize()];
                byte[] mp3Buffer = new byte[
                        Math.max(8192, encoder.getMP3BufferSize())
                ];

                int read;

                while ((read = input.read(pcmBuffer)) != -1) {
                    if (read == 0) {
                        continue;
                    }

                    int written = encoder.encodeBuffer(
                            pcmBuffer, 0, read, mp3Buffer
                    );

                    if (written < 0) {
                        throw new IOException("Không mã hóa được dữ liệu MP3");
                    }

                    output.write(mp3Buffer, 0, written);
                    checkSize(output.size());
                }

                int remaining = encoder.encodeFinish(mp3Buffer);

                if (remaining < 0) {
                    throw new IOException("Không hoàn tất được dữ liệu MP3");
                }

                output.write(mp3Buffer, 0, remaining);
                checkSize(output.size());

                if (output.size() == 0) {
                    throw new IOException("File MP3 được tạo bị rỗng");
                }

                return output.toByteArray();
            } finally {
                encoder.close();
            }

        } catch (UnsupportedAudioFileException exception) {
            throw new IOException("Không đọc được file WAV", exception);
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "Thông số âm thanh không được hỗ trợ", exception
            );
        }
    }

    private void checkSize(int size) throws IOException {
        if (size > MAX_MP3_SIZE) {
            throw new IOException("File MP3 vượt quá 20 MB");
        }
    }
}