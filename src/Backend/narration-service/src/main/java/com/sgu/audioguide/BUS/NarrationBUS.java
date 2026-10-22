package com.sgu.audioguide.BUS;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sgu.audioguide.DAO.NarrationDAO;
import com.sgu.audioguide.DTO.NarrationDTO;
import com.sgu.audioguide.DTO.NarrationWriteDTO;

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
    
        }}

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
}
