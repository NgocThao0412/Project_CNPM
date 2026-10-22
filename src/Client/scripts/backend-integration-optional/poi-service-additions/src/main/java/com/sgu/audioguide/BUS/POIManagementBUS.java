package com.sgu.audioguide.BUS;
import com.sgu.audioguide.DAO.*;
import com.sgu.audioguide.DTO.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service public class POIManagementBUS {
    private final POIManagementDAO dao;
    public POIManagementBUS(POIManagementDAO dao) {
        this.dao=dao;
    }
    public List<POIManagementDTO> list() {
        return dao.findAll();
    }
    public List<Map<String,Object>> areas() {
        return dao.areas();
    } private void require(Long id) {
        if(id==null||id<=0||!dao.exists(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"POI không tồn tại");
    } @Transactional public void update(Long id,CreatePOIRequestDTO request) {
        require(id);
        if(!dao.areaExists(request.areaId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Khu vực không tồn tại");
        dao.update(id,request);
    } @Transactional public void visibility(Long id,boolean value) {
        require(id);
        dao.visibility(id,value);
    }
}
