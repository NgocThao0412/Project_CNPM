package BUS;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import DAO.NarrationDAO;
import DTO.AdminNarrationWriteDTO;
import DTO.NarrationDTO;
import DTO.NarrationWriteDTO;

@Service
public class NarrationBUS {

    private final NarrationDAO dao;

    public NarrationBUS(NarrationDAO dao) {
        this.dao = dao;
    }

    public List<NarrationDTO> visible() {
        return dao.visible();
    }

    public List<NarrationDTO> all() {
        return dao.all();
    }

    public NarrationDTO create(NarrationWriteDTO n) {
        validate(n);
        return dao.create(n);
    }

    public NarrationDTO update(long id, NarrationWriteDTO n) {
        validate(n);
        return dao.update(id, n);
    }

    public void visibility(long id, boolean v) {
        if (dao.visible(id, v) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thuyết minh");

        }
    }

    private void validate(NarrationWriteDTO n) {
        if (n == null || n.poiId() == null || n.poiId() < 1 || n.languageCode() == null || n.languageCode().isBlank()
                || n.title() == null || n.title().isBlank() || n.title().length() > 200 || n.content() == null || n.content().isBlank()) {
            throw new IllegalArgumentException("Thiếu POI, ngôn ngữ, tiêu đề hoặc nội dung");
        }
    }

    public List<NarrationDTO> visible(Long poiId, String languageCode) {
        if (poiId != null && poiId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID POI phải lớn hơn 0"
            );
        }

        String normalizedCode = null;

        if (languageCode != null) {
            normalizedCode = languageCode.strip();

            if (normalizedCode.isEmpty() || normalizedCode.length() > 35) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Mã ngôn ngữ không hợp lệ"
                );
            }
        }

        return dao.visible(poiId, normalizedCode);
    }

    public NarrationDTO getAdminDetail(long id) {
        if (id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID thuyết minh phải lớn hơn 0"
            );
        }

        return dao.find(id).orElseThrow(()
                -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy thuyết minh"
                )
        );
    }

    private NarrationWriteDTO convertAdminRequest(
            AdminNarrationWriteDTO request,
            Boolean defaultVisible
    ) {
        if (request == null
                || request.languageId() == null
                || request.languageId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn ngôn ngữ hợp lệ"
            );
        }

        String languageCode = dao.findActiveLanguageCode(
                request.languageId()
        ).orElseThrow(()
                -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ngôn ngữ không tồn tại hoặc đã ngừng hỗ trợ"
                )
        );

        NarrationWriteDTO converted = new NarrationWriteDTO(
                request.poiId(),
                languageCode,
                request.title(),
                request.content(),
                request.visible() == null
                ? defaultVisible
                : request.visible()
        );

        try {
            validate(converted);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }

        return converted;
    }

    public NarrationDTO createFromAdmin(AdminNarrationWriteDTO request) {
        NarrationWriteDTO converted = convertAdminRequest(request, true);

        try {
            return create(converted);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "POI đã có nội dung thuyết minh cho ngôn ngữ này"
            );
        }
    }

    public NarrationDTO updateFromAdmin(
            long id,
            AdminNarrationWriteDTO request
    ) {
        NarrationDTO current = getAdminDetail(id);

        NarrationWriteDTO converted = convertAdminRequest(
                request,
                current.isVisible()
        );

        if (!current.poiId().equals(converted.poiId())
                || !current.languageCode().equals(converted.languageCode())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không được đổi POI hoặc ngôn ngữ của bài đã tạo"
            );
        }

        return update(id, converted);
    }
}
