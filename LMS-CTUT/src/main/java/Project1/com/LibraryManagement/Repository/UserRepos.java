package Project1.com.LibraryManagement.Repository;

import Project1.com.LibraryManagement.Entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepos extends JpaRepository<Users,Long> {
    Boolean existsByEmail(String email);
    Optional<Users> findByEmail(String email);
    Optional<Users> findById(Long Id);
    Users findByFullNameAndPhoneNumber(String fullName, String phoneNumber);
    Optional<Users> findByGoogleId(String googleId);

    @Query(
            value = """
        SELECT u.*
        FROM users u
        JOIN unit un ON u.unit_id = un.id
        WHERE
            (:keyword IS NULL OR :keyword = ''
             OR MATCH(u.email, u.fullname, u.phonenumber)
                AGAINST(:keyword IN NATURAL LANGUAGE MODE))
        AND (:userStatus IS NULL OR :userStatus = ''
             OR u.userstatus = :userStatus)
        AND (:unitId IS NULL OR un.id = :unitId)
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM users u
        JOIN unit un ON u.unit_id = un.id
        WHERE
            (:keyword IS NULL OR :keyword = ''
             OR MATCH(u.email, u.fullname, u.phonenumber)
                AGAINST(:keyword IN NATURAL LANGUAGE MODE))
        AND (:userStatus IS NULL OR :userStatus = ''
             OR u.userstatus = :userStatus)
        AND (:unitId IS NULL OR un.id = :unitId)
        """,
            nativeQuery = true
    )
    Page<Users> searchUsers(
            @Param("keyword") String keyword,
            @Param("userStatus") String userStatus,
            @Param("unitId") Long unitId,
            Pageable pageable
    );
}
