package Project1.com.LibraryManagement.Controller;

import Project1.com.LibraryManagement.DTO.BorrowingDTO;
import Project1.com.LibraryManagement.Entity.*;
import Project1.com.LibraryManagement.Repository.*;
import Project1.com.LibraryManagement.Service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/librarian")
public class LibrarianController {
    @Autowired private BookService bookService;
    @Autowired private BookRepos bookRepos;
    @Autowired private AuthorRepos authorRepos;

    @Autowired private UserService userService;
    @Autowired private UserRepos userRepos;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private BorrowRequestService borrowRequestService;
    @Autowired private BorrowRequestRepos borrowRequestRepos;

    @Autowired private BorrowingService borrowingService;
    @Autowired private BorrowingRepos borrowingRepos;

    @Autowired private BorrowRecordService borrowRecordService;
    @Autowired private BorrowRecordRepos borrowRecordRepos;

    @Autowired private BookCopyRepos bookCopyRepos;

    @Autowired private FileUpload fileUpload;
    @Autowired private GoogleBookService googleBookService;
    @Autowired private CustomUserDetailService customUserDetailService;
    @Autowired private UnitRepos unitRepos;

    @GetMapping("/loginLib")
    public String loginLibrarian() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/librarian/dashboard";
        }
        return "librarian/loginLib";
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "librarian/dashboard";
    }

    @GetMapping("/readers")
    public String readers(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) Long id,
                          Model model) {

        Page<Users> usersPage = userRepos.findAll(PageRequest.of(page, pageSize));

        model.addAttribute("userPageContent", usersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", usersPage.getTotalPages());

        List<Unit> units = unitRepos.findAll()
                .stream()
                .filter(u ->
                        !"No".equals(u.getName()) &&
                                !"Phòng quản lý".equals(u.getName())
                )
                .toList();
        model.addAttribute("units", units);


        if (id != null) {
            Users editUsers = userRepos.findById(id).orElse(null);
            model.addAttribute("editUsers", editUsers);
            model.addAttribute("showEditModal", true);
        }

        if (!model.containsAttribute("editUsers")) {
            model.addAttribute("editUsers", new Users());
        }
        model.addAttribute("isSearching", false);

        return "librarian/readers";
    }

    @PostMapping("/saveUsers")
    public String saveUsers(@RequestParam("fullName") String fullName,
                            @RequestParam("email") String email,
                            @RequestParam("phoneNumber") String phoneNumber,
                            @RequestParam(value = "id", required = false) Long unitId,
                            @RequestParam("dayofBirth") Integer dayofBirth,
                            @RequestParam("monthofBirth") Integer monthofBirth,
                            @RequestParam("yearofBirth") Integer yearofBirth,
                            @RequestParam("address") String address,
                            @RequestParam("password") String password,
                            @RequestParam("roles") Roles roles,
                            @RequestParam("userStatus") String userStatus, RedirectAttributes redirectAttributes, Model model) {

        if (!phoneNumber.matches("\\d{10}")) {
            redirectAttributes.addFlashAttribute("errorPhoneNumber", "Số điện thoại phải đủ 10 số");
            return "librarian/readers";
        }

        try {
            if (userRepos.existsByEmail(email)) {
                redirectAttributes.addFlashAttribute("errorExistsEmail", "Tài khoản này đã tồn tại");
                return "redirect:/librarian/readers";
            }
            LocalDate dateOfBirth;
            try {
                dateOfBirth = LocalDate.of(yearofBirth, monthofBirth, dayofBirth);
            } catch (DateTimeException ex) {
                redirectAttributes.addFlashAttribute("errorDateOfBirth", "Ngày tháng năm sinh không hợp lệ");
                return "user/readers";
            }
            Users users = new Users();
            users.setEmail(email);
            users.setPassword(passwordEncoder.encode(password));
            users.setFullName(fullName);
            users.setPhoneNumber(phoneNumber);
            Unit unit = unitRepos.findById(unitId)
                    .orElseThrow(() -> new RuntimeException("Unit not found"));
            users.setUnit(unit);
            users.setAddress(address);
            users.setDateOfBirth(dateOfBirth);
            users.setUserStatus(userStatus);
            users.setRoles(roles);
            Users saved = userService.saveUser(users);

            if (saved == null || saved.getId() == null) {
                redirectAttributes.addFlashAttribute("errorSaveUser", "Lỗi lưu thông tin người dùng! thử lại");
                return "user/readers";
            }
            redirectAttributes.addFlashAttribute("successSaveUser", "Lưu thông tin người dùng thành công");
            return "redirect:/librarian/readers";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("ErrorAddUser", "Error creating account: " + e.getMessage());
            return "librarian/readers";
        }

    }
    @PostMapping("/saveEditUsers")
    public String saveEditUsers(@RequestParam("id") Long id,
                                @RequestParam("email") String email,
                                @RequestParam(value = "unitId", required = false) Long unitId,
                                @RequestParam("fullName") String fullName,
                                @RequestParam("phoneNumber") String phoneNumber,
                                @RequestParam("address") String address,
                                @RequestParam("userStatus") String userStatus,
                                @RequestParam("roles") Roles roles,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                RedirectAttributes redirectAttributes,
                                Model model) {

        try {
            Optional<Users> usersOpt = userService.findById(id);
            if (usersOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorEditUser", "Người dùng không tìm thấy");
                return "redirect:/librarian/readers?page=" + page;
            }

            Users user = usersOpt.get();

            if (userService.existsUser(email) && !user.getEmail().equals(email)) {
                redirectAttributes.addFlashAttribute("errorExistUser", "Tài khoản email này đã tồn tại");
                redirectAttributes.addFlashAttribute("editUser", user);
                return "librarian/readers";
            }
            user.setEmail(email);
            Unit unit = unitRepos.findById(unitId)
                    .orElseThrow(() -> new RuntimeException("Unit not found"));
            user.setUnit(unit);
            user.setFullName(fullName);
            user.setPhoneNumber(phoneNumber);
            user.setAddress(address);
            user.setUserStatus(userStatus);
            user.setRoles(roles);
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("successEditUser", "Cập nhật thông tin thành công!");
            return "redirect:/librarian/readers?page=" + page;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorEditUser", "Error updating user information: " + e.getMessage());
            return "librarian/readers";
        }
    }
    @GetMapping("/searchUsers")
    public String searchUsers(Model model,
                              @RequestParam(value = "keyword", required = false) String keyword,
                              @RequestParam(value = "userStatus", required = false) String userStatus,
                              @RequestParam(value = "unitId", required = false) Long unitId,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int pageSize) {

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String st = (userStatus != null && !userStatus.isBlank()) ? userStatus.trim() : null;

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Users> usersPage;

        boolean hasFilter =
                kw != null || st != null || unitId != null;

        if (hasFilter) {
            usersPage = userRepos.searchUsers(kw, st, unitId, pageable);
        } else {
            usersPage = userRepos.findAll(pageable);
        }

        model.addAttribute("userPageContent", usersPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", usersPage.getTotalPages());

        model.addAttribute("keyword", kw);
        model.addAttribute("userStatus", st);
        model.addAttribute("unitId", unitId);
        if (!model.containsAttribute("editUsers")) {
            model.addAttribute("editUsers", new Users());
        }
        return "librarian/readers";
    }
    @GetMapping("/deleteUsers")
    public String deleteUsers(@RequestParam("id") Long id,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              RedirectAttributes redirectAttributes) {
        userService.deleteUsers(id);
        redirectAttributes.addFlashAttribute("deleteUserSuccess", "Xóa người dùng thành công");
        return "redirect:/librarian/readers?page=" + page;
    }
    /* ================= BOOKS (METADATA ONLY) ================= */

    @GetMapping("/books")
    public String books(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int pageSize,
                        Model model) {

        Page<Books> booksPage = bookRepos.findAll(PageRequest.of(page, pageSize));

        model.addAttribute("bookPageContent", booksPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", booksPage.getTotalPages());

        return "librarian/books";
    }

    @PostMapping("/saveBooks")
    public String saveBooks(@RequestParam String bookCode,
                            @RequestParam String bookName,
                            @RequestParam String authorName,
                            @RequestParam String isbn,
                            @RequestParam String category,
                            @RequestParam double price,
                            @RequestParam String status,
                            @RequestParam MultipartFile image,
                            @RequestParam String description,
                            RedirectAttributes redirect) throws IOException {

        if (bookService.existsBooks(bookCode, bookName)) {
            redirect.addFlashAttribute("error", "Sách đã tồn tại");
            return "redirect:/librarian/books";
        }

        Books book = new Books();
        book.setBookCode(bookCode);
        book.setBookName(bookName);
        book.setIsbn(isbn);
        book.setPrice(price);
        book.setStatus(status);
        book.setDescription(description);

        if (!image.isEmpty()) {
            book.setImage(fileUpload.uploadFile(image));
        }

        Author author = authorRepos.findByName(authorName.trim());
        if (author == null) {
            author = new Author();
            author.setName(authorName.trim());
            author = authorRepos.save(author);
        }
        book.setAuthors(Set.of(author));

        bookService.saveBooks(book);
        redirect.addFlashAttribute("success", "Thêm sách thành công");

        return "redirect:/librarian/books";
    }

    /* ================= CIRCULATION ================= */

    @GetMapping("/circulation")
    public String circulation(Model model) {

        model.addAttribute("borrowRequest", borrowRequestService.getAllRequest());
        model.addAttribute("borrowing", borrowingService.getAllBorrowing());
        model.addAttribute("borrowRecord", borrowRecordService.getAllRecord());

        List<BorrowRecord> overdueList = borrowRecordService.getAllRecord()
                .stream()
                .filter(r -> borrowRecordService.getRealTimeFine(r) > 0)
                .filter(r -> r.getStatus() != RecordStatus.RETURNED)
                .toList();

        model.addAttribute("overdueList", overdueList);

        return "librarian/circulation";
    }

    /* ================= ACCEPT BORROW ================= */

    @PostMapping("/accept")
    public String acceptBorrow(@ModelAttribute BorrowingDTO borrowingDTO,
                               BindingResult result,
                               RedirectAttributes redirect) {

        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", "Lỗi duyệt mượn");
            return "redirect:/librarian/circulation";
        }

        Borrowing borrowing = borrowingService.createBorrowing(borrowingDTO);

        customUserDetailService.sendMailApprove(
                borrowing.getUsers(),
                borrowing.getCreatedAt(),
                borrowing.getDeadline()
        );

        redirect.addFlashAttribute("success", "Đã duyệt mượn sách");
        return "redirect:/librarian/circulation";
    }

    /* ================= REJECT BORROW ================= */

    @PostMapping("/reject")
    @Transactional
    public String rejectBorrow(@RequestParam Long requestId,
                               RedirectAttributes redirect) {

        BorrowRequest request = borrowRequestRepos.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        BookCopy bookCopy = request.getBookCopy();
        bookCopy.setStatus(BookCopyStatus.AVAILABLE);
        bookCopyRepos.save(bookCopy);

        customUserDetailService.sendMailReject(request.getUsers());
        borrowRequestRepos.delete(request);

        redirect.addFlashAttribute("success", "Đã từ chối yêu cầu mượn");
        return "redirect:/librarian/circulation";
    }

    /* ================= RETURN BOOK ================= */

    @PostMapping("/returned")
    @Transactional
    public String returned(@RequestParam List<Long> ids,
                           RedirectAttributes redirect) {

        for (BorrowRecord record : borrowRecordRepos.findAllById(ids)) {
            borrowRecordService.processReturn(record);
        }

        redirect.addFlashAttribute("success", "Đã trả sách thành công");
        return "redirect:/librarian/circulation";
    }

    /* ================= EXPORT / IMPORT ================= */

    @GetMapping("/exportBooks")
    public void exportBooks(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=books.csv");
        bookService.exportBooksToCsv(response.getWriter());
    }

    @PostMapping("/importBooks")
    public String importBooks(@RequestParam MultipartFile file,
                              RedirectAttributes redirect) {

        try {
            bookService.importBooksFromCsv(file);
            redirect.addFlashAttribute("success", "Import CSV thành công");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/books";
    }
}
