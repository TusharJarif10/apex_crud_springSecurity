package com.apex.tushar.service;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.apex.tushar.config.JWTAuthFilter;
import com.apex.tushar.dto.RequestResponseDto;
import com.apex.tushar.entity.Users;
import com.apex.tushar.repository.UsersRepo;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class UsersManagementService {

    @Autowired
    private UsersRepo usersRepo;
    @Autowired
    private JWTUtils jwtUtils;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;


    public RequestResponseDto register(RequestResponseDto registrationRequest){
        RequestResponseDto resp = new RequestResponseDto();

        try {
            Users ourUser = new Users();
            ourUser.setEmail(registrationRequest.getEmail());
            ourUser.setDesignation(registrationRequest.getDesignation());
            ourUser.setDeptmstcode(registrationRequest.getDeptmstcode());
            ourUser.setRole(registrationRequest.getRole());
            ourUser.setName(registrationRequest.getName());
            ourUser.setUserid(registrationRequest.getUserid());
            ourUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
            Users ourUsersResult = usersRepo.save(ourUser);
            if (ourUsersResult.getId()>0) {
                resp.setOurUsers((ourUsersResult));
                resp.setMessage("User Saved Successfully");
                resp.setStatusCode(200);
            }

        }catch (Exception e){
            resp.setStatusCode(500);
            resp.setError(e.getMessage());
        }
        return resp;
    }


    public RequestResponseDto login(RequestResponseDto loginRequest){
        RequestResponseDto response = new RequestResponseDto();
        try {
            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUserid(),
                            loginRequest.getPassword()));
            var user = usersRepo.findByUserid(loginRequest.getUserid()).orElseThrow();
            var jwt = jwtUtils.generateToken(user);
            var refreshToken = jwtUtils.generateRefreshToken(new HashMap<>(), user);
            response.setStatusCode(200);
            response.setToken(jwt);
            response.setRole(user.getRole());
            response.setRefreshToken(refreshToken);
            response.setExpirationTime("24Hrs");
            response.setMessage("Successfully Logged In");

        }catch (Exception e){
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
        }
        return response;
    }





    public RequestResponseDto refreshToken(RequestResponseDto refreshTokenReqiest){
        RequestResponseDto response = new RequestResponseDto();
        try{
            String ourUserid = jwtUtils.extractUsername(refreshTokenReqiest.getToken());
            Users users = usersRepo.findByUserid(ourUserid).orElseThrow();
            if (jwtUtils.isTokenValid(refreshTokenReqiest.getToken(), users)) {
                var jwt = jwtUtils.generateToken(users);
                response.setStatusCode(200);
                response.setToken(jwt);
                response.setRefreshToken(refreshTokenReqiest.getToken());
                response.setExpirationTime("24Hr");
                response.setMessage("Successfully Refreshed Token");
            }
            response.setStatusCode(200);
            return response;

        }catch (Exception e){
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
            return response;
        }
    }

    public RequestResponseDto getUsersWithPaginationAndSorting(int page, int size, String sortBy, String search) {
        RequestResponseDto requestResponseDto = new RequestResponseDto();
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
            Specification<Users> spec = (root, query, criteriaBuilder) -> {
                if (search != null && !search.isEmpty()) {
                    return criteriaBuilder.or(
                            criteriaBuilder.like(root.get("name"), "%" + search + "%"),
                            criteriaBuilder.like(root.get("email"), "%" + search + "%"),
                            criteriaBuilder.like(root.get("designation"), "%" + search + "%"),
                            criteriaBuilder.like(root.get("deptmstcode"), "%" + search + "%"),
                            criteriaBuilder.like(root.get("userid"), "%" + search + "%")
                    );
                }
                return criteriaBuilder.conjunction(); // return true if no search is provided
            };

            Page<Users> usersPage = usersRepo.findAll(spec, pageable);
            if (usersPage.hasContent()) {
                requestResponseDto.setOurUsersList(usersPage.getContent());
                requestResponseDto.setStatusCode(200);
                requestResponseDto.setMessage("Users retrieved successfully");
                requestResponseDto.setTotalElements(usersPage.getTotalElements());
                requestResponseDto.setTotalPages(usersPage.getTotalPages());
            } else {
                requestResponseDto.setStatusCode(404);
                requestResponseDto.setMessage("No users found");
            }
        } catch (Exception e) {
            requestResponseDto.setStatusCode(500);
            requestResponseDto.setMessage("Error occurred: " + e.getMessage());
        }
        return requestResponseDto;
    }



    public RequestResponseDto getAllUsers() {
        RequestResponseDto requestResponseDto = new RequestResponseDto();

        try {
            List<Users> result = usersRepo.findAll();
            if (!result.isEmpty()) {
                requestResponseDto.setOurUsersList(result);
                requestResponseDto.setStatusCode(200);
                requestResponseDto.setMessage("Successful");
            } else {
                requestResponseDto.setStatusCode(404);
                requestResponseDto.setMessage("No users found");
            }
            return requestResponseDto;
        } catch (Exception e) {
            requestResponseDto.setStatusCode(500);
            requestResponseDto.setMessage("Error occurred: " + e.getMessage());
            return requestResponseDto;
        }
    }


    public RequestResponseDto getUsersById(Integer id) {
        RequestResponseDto requestResponseDto = new RequestResponseDto();
        try {
            Users usersById = usersRepo.findById(id).orElseThrow(() -> new RuntimeException("User Not found"));
            requestResponseDto.setOurUsers(usersById);
            requestResponseDto.setStatusCode(200);
            requestResponseDto.setMessage("Users with id '" + id + "' found successfully");
        } catch (Exception e) {
            requestResponseDto.setStatusCode(500);
            requestResponseDto.setMessage("Error occurred: " + e.getMessage());
        }
        return requestResponseDto;
    }


    public RequestResponseDto deleteUser(Integer userId) {
        RequestResponseDto requestResponseDto = new RequestResponseDto();
        try {
            Optional<Users> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                usersRepo.deleteById(userId);
                requestResponseDto.setStatusCode(200);
                requestResponseDto.setMessage("User deleted successfully");
            } else {
                requestResponseDto.setStatusCode(404);
                requestResponseDto.setMessage("User not found for deletion");
            }
        } catch (Exception e) {
            requestResponseDto.setStatusCode(500);
            requestResponseDto.setMessage("Error occurred while deleting user: " + e.getMessage());
        }
        return requestResponseDto;
    }

    public RequestResponseDto updateUser(Integer userId, Users updatedUser) {
        RequestResponseDto requestResponseDto = new RequestResponseDto();
        try {
            Optional<Users> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                Users existingUser = userOptional.get();
                existingUser.setEmail(updatedUser.getEmail());
                existingUser.setName(updatedUser.getName());
                existingUser.setUserid(updatedUser.getUserid());
                existingUser.setDesignation(updatedUser.getDesignation());
                existingUser.setDeptmstcode(updatedUser.getDeptmstcode());

                existingUser.setRole(updatedUser.getRole());

                // Check if password is present in the request
                if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                    // Encode the password and update it
                    existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                }

                Users savedUser = usersRepo.save(existingUser);
                requestResponseDto.setOurUsers(savedUser);
                requestResponseDto.setStatusCode(200);
                requestResponseDto.setMessage("User updated successfully");
            } else {
                requestResponseDto.setStatusCode(404);
                requestResponseDto.setMessage("User not found for update");
            }
        } catch (Exception e) {
            requestResponseDto.setStatusCode(500);
            requestResponseDto.setMessage("Error occurred while updating user: " + e.getMessage());
        }
        return requestResponseDto;
    }


    public RequestResponseDto getMyInfo(String userid){
        RequestResponseDto requestResponseDto = new RequestResponseDto();
        try {
            Optional<Users> userOptional = usersRepo.findByUserid(userid);
            if (userOptional.isPresent()) {
                requestResponseDto.setOurUsers(userOptional.get());
                requestResponseDto.setStatusCode(200);
                requestResponseDto.setMessage("successful");
            } else {
                requestResponseDto.setStatusCode(404);
                requestResponseDto.setMessage("User not found for update");
            }

        }catch (Exception e){
            requestResponseDto.setStatusCode(500);
            requestResponseDto.setMessage("Error occurred while getting user info: " + e.getMessage());
        }
        return requestResponseDto;

    }
}