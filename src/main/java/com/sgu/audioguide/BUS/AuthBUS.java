package com.sgu.audioguide.BUS;

import com.sgu.audioguide.DAO.AdminDAO;
import com.sgu.audioguide.DTO.AdminDTO;
import com.sgu.audioguide.DTO.LoginRequestDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthBUS {

        private final AdminDAO adminDAO;
        private final PasswordEncoder passwordEncoder;

        public AuthBUS(AdminDAO adminDAO, PasswordEncoder passwordEncoder) {
                this.adminDAO = adminDAO;
                this.passwordEncoder = passwordEncoder;
        }

        public AdminDTO getAuthenticatedAdmin(Long adminId, long authVersion) {
                AdminDTO admin = adminDAO.findById(adminId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Phiên đăng nhập không hợp lệ"));

                if (!admin.isActive() || admin.getAuthVersion() != authVersion) {
                        throw new IllegalArgumentException(
                                        "Phiên đăng nhập không hợp lệ");
                }

                return admin;
        }

        public AdminDTO authenticate(LoginRequestDTO request) {
                if (request == null
                                || request.getUsername() == null
                                || request.getUsername().isBlank()
                                || request.getPassword() == null
                                || request.getPassword().isBlank()) {
                        throw new IllegalArgumentException(
                                        "Vui lòng nhập tên đăng nhập và mật khẩu");
                }

                AdminDTO admin = adminDAO
                                .findByUsername(request.getUsername().trim())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Thông tin đăng nhập không hợp lệ"));

                boolean passwordMatches = passwordEncoder.matches(
                                request.getPassword(),
                                admin.getPasswordHash());

                if (!passwordMatches || !admin.isActive()) {
                        throw new IllegalArgumentException(
                                        "Thông tin đăng nhập không hợp lệ");
                }

                return admin;
        }
}