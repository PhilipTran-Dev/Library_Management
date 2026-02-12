package Project1.com.LibraryManagement.Service;

import Project1.com.LibraryManagement.Entity.Roles;
import Project1.com.LibraryManagement.Entity.Unit;
import Project1.com.LibraryManagement.Entity.Users;
import Project1.com.LibraryManagement.Repository.UnitRepos;
import Project1.com.LibraryManagement.Repository.UserRepos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class CustomOAuth2UserServiceImpl implements CustomOAuth2UserService {

    @Autowired
    private UserRepos userRepos;
    @Autowired
    private UnitRepos unitRepos;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");

        Optional<Users> existingUser = userRepos.findByEmail(email);
        if (existingUser.isEmpty()) {
            Users newUser = new Users();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setFullName(fullName);
            newUser.setPassword(null);
            newUser.setAddress(null);
            newUser.setPhoneNumber(null);
            newUser.setDateOfBirth(null);
            newUser.setUnit(null);
            newUser.setUserStatus("Hoạt động");
            newUser.setRoles(Roles.USERS);

            userRepos.save(newUser);
            System.out.println("Created new Google user: " + email);
        }
        else {
            System.out.println("User already exists, skip insert: " + email);
        }

        return oAuth2User;
    }


}
