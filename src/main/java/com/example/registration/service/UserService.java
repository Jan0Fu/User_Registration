package com.example.registration.service;

import com.example.registration.email.EmailSender;
import com.example.registration.entity.ConfirmationToken;
import com.example.registration.entity.User;
import com.example.registration.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {

    private final static String USER_NOT_FOUND_MSG = "user with email %s not found";
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailSender emailSender;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(() ->
                new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, email)));
    }

    public String signUpUser(User user) {
        Optional<User> savedUser = userRepository.findByEmail(user.getEmail());
        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15),
                user
        );
        if (savedUser.isPresent()) {
            if (savedUser.get().equals(user) && (!user.isEnabled())) sendMail(user, token);
            // TODO check if attributes are the same and
            // TODO if email not confirmed send confirmation mail
            throw new IllegalStateException("email already taken");
        }
        String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        userRepository.save(user);
        confirmationTokenService.saveConfirmationToken(confirmationToken);
        return token;
    }

    public void sendMail(User user, String token) {
        String link = "http://localhost:8080/api/v1/registration/confirm?token=" + token;
        emailSender.send(user.getEmail(), emailSender.buildEmail(user.getfirstName(), link));
    }

    public int enableUser(String email) {
        return userRepository.enableAppUser(email);
    }
}
