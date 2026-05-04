package org.pfe.ppm_project.services;

import org.pfe.ppm_project.entities.User;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    User createUser(User user);

    List<User> getAllUsers();

    Optional<User> getUserById(Long id);

    Optional<User> getUserByEmail(String email);

    User updateUser(Long id, User user);

    void deactivateUser(Long id);

    void setUserActive(Long id, boolean active);

    void deleteUser(Long id);
}
