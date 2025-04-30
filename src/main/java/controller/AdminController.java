package com.ecom.controller;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.ReadOnline;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.ReadOnlineService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private CartService cartService;

	@Autowired
	private OrderService orderService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@ModelAttribute
	public void getUserDetails(Principal p, Model m) {

		if (p != null) {

			String email = p.getName();
			UserDtls userDtls = userService.getUserByEmail(email);
			m.addAttribute("user", userDtls);
			Integer countCart = cartService.getCountCart(userDtls.getId());
			m.addAttribute("countCart", countCart);
		}

		List<Category> allActiveCategory = categoryService.getAllActiveCategory();
		m.addAttribute("categorys", allActiveCategory);

	}

	@GetMapping("/")
	public String index() {
		return "admin/index";
	}

	@GetMapping("/loadAddProduct")
	public String loadAddProduct(Model m) {
		List<Category> categories = categoryService.getAllCategory();
		m.addAttribute("categories", categories);
		return "admin/add_product";
	}

	@GetMapping("/category")
	public String category(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {

		// m.addAttribute("categorys", categoryService.getAllCategory());

		Page<Category> page = categoryService.getAllCategoryPagination(pageNo, pageSize);

		List<Category> categorys = page.getContent();
		m.addAttribute("categorys", categorys);
		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/category";
	}

	@PostMapping("/saveCategory")
	public String saveCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {

		String imageName = file != null ? file.getOriginalFilename() : "default.jpg";
		category.setImageName(imageName);

		Boolean existCategory = categoryService.existCategory(category.getName());

		if (existCategory) {

			session.setAttribute("errorMsg", "Category Name Already Exists");
		} else {
			Category saveCategory = categoryService.saveCategory(category);

			if (ObjectUtils.isEmpty(saveCategory)) {
				session.setAttribute("errorMsg", "Not Saved ! Internal server error");
			} else {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());
				System.out.println(path);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				session.setAttribute("succMsg", "Saved successfully");
			}
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/deleteCategory/{id}")
	public String deleteCategory(@PathVariable int id, HttpSession session) {
		Boolean deleteCategory = categoryService.deleteCategory(id);
		if (deleteCategory) {
			session.setAttribute("succMsg", "Category Deleted Successfully");
		} else {
			session.setAttribute("errorMsg", "Something Went Wrong");
		}

		return "redirect:/admin/category";
	}

	@GetMapping("/loadEditCategory/{id}")
	public String loadEditCategory(@PathVariable int id, Model m) {
		m.addAttribute("category", categoryService.getCategoryById(id));
		return "admin/edit_category";
	}

	@PostMapping("/updateCategory")
	public String updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,
			HttpSession session) throws IOException {
		Category Oldcategory = categoryService.getCategoryById(category.getId());
		String imageName = file.isEmpty() ? Oldcategory.getImageName() : file.getOriginalFilename();

		if (!ObjectUtils.isEmpty(category)) {

			Oldcategory.setName(category.getName());
			Oldcategory.setIsActive(category.getIsActive());
			Oldcategory.setImageName(imageName);
		}

		Category updateCategory = categoryService.saveCategory(Oldcategory);

		if (!ObjectUtils.isEmpty(updateCategory)) {

			if (!file.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "category_img" + File.separator
						+ file.getOriginalFilename());
				System.out.println(path);

//				print path
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
			session.setAttribute("succMsg", "Category Updated Successfully");

		} else {
			session.setAttribute("succMsg", "Something Went Wrong");

		}

		return "redirect:/admin/loadEditCategory/" + category.getId();
	}

	@PostMapping("/saveProduct")
	public String saveProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session) throws IOException {

		String imageName = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();

		product.setImage(imageName);
		product.setDiscount(0);
		product.setDiscountPrice(product.getPrice());
		Product saveProduct = productService.saveProduct(product);

		if (!ObjectUtils.isEmpty(saveProduct)) {
			File saveFile = new ClassPathResource("static/img").getFile();

			Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "product_img" + File.separator
					+ image.getOriginalFilename());
			// System.out.println(path);

			Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

			session.setAttribute("succMsg", "Product Saved Successfully");
		} else {
			session.setAttribute("errorMsg", "Something went wrong on server");
		}

		return "redirect:/admin/loadAddProduct";
	}

	@GetMapping("/products")
	public String loadViewProduct(Model m, @RequestParam(defaultValue = "") String ch, HttpSession session,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {

		Page<Product> page = null;
		if (ch != null && ch.length() > 0) {
			page = productService.searchProductPagination(pageNo, pageSize, ch);
			if (page == null || page.isEmpty()) {
				session.setAttribute("errorMsg", "Incorrect search !! No products found.");
			} else {
				session.removeAttribute("errorMsg");
			}
		} else {
			page = productService.getAllProductsPagination(pageNo, pageSize);
			session.removeAttribute("errorMsg");
		}
		m.addAttribute("products", page.getContent());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "admin/products";
	}

	@GetMapping("/deleteProduct/{id}")
	public String deleteProduct(@PathVariable int id, HttpSession session) {

		Boolean deleteProduct = productService.deleteProduct(id);
		if (deleteProduct) {
			session.setAttribute("succMsg", "Product Deleted Successfully");
		} else {
			session.setAttribute("errorMsg", "Something Went Wrong");
		}

		return "redirect:/admin/products";
	}

	@GetMapping("/editProduct/{id}")
	public String loadEditProduct(@PathVariable int id, Model m) {
		m.addAttribute("product", productService.getProductById(id));
		m.addAttribute("categories", categoryService.getAllCategory());
		return "admin/edit_product";
	}

	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product, @RequestParam("file") MultipartFile image,
			HttpSession session) throws IOException {

		if (product.getDiscount() < 0 || product.getDiscount() > 100) {
			session.setAttribute("errorMsg", "Invalid Discount");

		} else {
			Product updateProduct = productService.updateProduct(product, image);

			if (!ObjectUtils.isEmpty(updateProduct)) {
				session.setAttribute("succMsg", "Product Updated Successfully");
			} else {
				session.setAttribute("errorMsg", "Something Went Wrong");
			}
		}

		return "redirect:/admin/editProduct/" + product.getId();
	}

	@GetMapping("/users")
	public String getAllUsers(Model m, @RequestParam Integer type) {

		List<UserDtls> users = null;

		if (type == 1) {
			users = userService.getUsers("ROLE_USER");

		} else {
			users = userService.getUsers("ROLE_ADMIN");
		}
		m.addAttribute("userType", type);
		m.addAttribute("users", users);
		return "/admin/users";
	}

	@GetMapping("/updateSts")
	public String updateUserAccountStatus(@RequestParam Boolean status, @RequestParam Integer id,
			@RequestParam Integer type, HttpSession session) {

		Boolean f = userService.updateAccountStatus(id, status);

		if (f) {
			session.setAttribute("succMsg", "Account Status Updated !");
		} else {
			session.setAttribute("errorMsg", "Something went wrong !");

		}
		return "redirect:/admin/users?type=" + type;

	}

	@GetMapping("/orders")
	public String getAllOrders(Model m, @RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {
//		List<ProductOrder> orders = orderService.getAllOrders();
//
//		orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
//
//		m.addAttribute("orders", orders);
//		m.addAttribute("srch", false);

		Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "orderDate"));

		Page<ProductOrder> page = orderService.getAllOrdersPagination(pageable);

		// Manually sort the content of the page using streams
		List<ProductOrder> sortedOrders = page.getContent().stream()
				.sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate())) // Sort by orderDate descending
				.collect(Collectors.toList());

		m.addAttribute("orders", sortedOrders);
		m.addAttribute("srch", false);

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "/admin/orders";
	}

	@PostMapping("/update-order-status")
	public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {

		OrderStatus[] values = OrderStatus.values();

		String status = null;

		for (OrderStatus orderSt : values) {

			if (orderSt.getId().equals(st)) {
				status = orderSt.getName();
			}

		}

		ProductOrder updateOrder = orderService.updateOrderStatus(id, status);

		try {
			commonUtil.sendMailForProductOrder(updateOrder, status);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!ObjectUtils.isEmpty(updateOrder)) {
			session.setAttribute("succMsg", "Order Updated Successfully !");

		} else {
			session.setAttribute("errorMsg", "status not updated");

		}

		return "redirect:/admin/orders";
	}

	// search product
	@GetMapping("/search-order")
	public String searchProduct(@RequestParam String orderId, Model m, HttpSession session,
			@RequestParam(name = "pageNo", defaultValue = "0") Integer pageNo,
			@RequestParam(name = "pageSize", defaultValue = "5") Integer pageSize) {

		if (orderId != null && orderId.length() > 0) {
			ProductOrder order = orderService.getOrderByOrderId(orderId.trim());

			if (ObjectUtils.isEmpty(order)) {

				session.setAttribute("errorMsg", "Incorrect Order Id !!!");
				m.addAttribute("orderDtls", null);
			} else {
				m.addAttribute("orderDtls", order);
			}

			m.addAttribute("srch", true);
		} else {
			Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo, pageSize);
			m.addAttribute("orders", page);
			m.addAttribute("srch", false);

			m.addAttribute("pageNo", page.getNumber());
			m.addAttribute("pageSize", pageSize);
			m.addAttribute("totalElements", page.getTotalElements());
			m.addAttribute("totalPages", page.getTotalPages());
			m.addAttribute("isFirst", page.isFirst());
			m.addAttribute("isLast", page.isLast());

		}
		return "/admin/orders";
	}

	// search product
	@GetMapping("/search")
	public String searchProduct(@RequestParam String ch, Model m) {
		List<Product> searchProduct = productService.searchProduct(ch);
		m.addAttribute("products", searchProduct);

		return "admin/products";
	}

	@GetMapping("/add-admin")
	public String loadAdminAdd() {

		return "/admin/add_admin";
	}

	@PostMapping("/save-admin")
	public String saveAdmin(@ModelAttribute UserDtls user, @RequestParam("img") MultipartFile file, HttpSession session)
			throws IOException {

		Boolean existsEmail = userService.existsEmail(user.getEmail());

		if (existsEmail) {
			session.setAttribute("errorMsg", "Admin Email already exist");
		} else {

			String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
			user.setProfileImage(imageName);

			UserDtls saveUser = userService.saveAdmin(user);

			if (!ObjectUtils.isEmpty(saveUser)) {
				if (!file.isEmpty()) {

					File saveFile = new ClassPathResource("static/img").getFile();

					Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
							+ file.getOriginalFilename());
					// System.out.println(path);

					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				}
				session.setAttribute("succMsg", "Registration Successfull !");

			} else {
				session.setAttribute("succMsg", "Something Went Wrong");
			}
		}

		return "redirect:/admin/add-admin";
	}

	@GetMapping("/profile")
	public String profile() {
		return "/admin/profile";
	}

	@PostMapping("/update-profile")
	public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session) {

		UserDtls updateUserProfile = userService.updateUserProfile(user, img);

		if (ObjectUtils.isEmpty(updateUserProfile)) {
			session.setAttribute("succMsg", "Profile Updated Successfully !");

		} else {
			session.setAttribute("errorMsg", "Something went wrong on update");

		}
		return "redirect:/admin/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam String newPassword, @RequestParam String currentPassword, Principal p,
			HttpSession session) {

		UserDtls loggedInUserDetails = commonUtil.getLoggedInUserDetails(p); // Method to get logged-in user details

		boolean matches = passwordEncoder.matches(currentPassword, loggedInUserDetails.getPassword());

		if (matches) {

			String encodePassword = passwordEncoder.encode(newPassword);
			loggedInUserDetails.setPassword(encodePassword);
			UserDtls updateUser = userService.updateUser(loggedInUserDetails);

			if (ObjectUtils.isEmpty(updateUser)) {
				session.setAttribute("errorMsg", "Password not updated || Error in Server");
			} else {
				session.setAttribute("succMsg", "Password Updated Successfully !");
			}

		} else {
			session.setAttribute("errorMsg", "Current Password is incorrect");

		}
		return "redirect:/admin/profile";
	}

	@GetMapping("/product-analytics")
	public String productAnalytics(Model model) {
		List<Product> products = productService.getAllProducts();
		List<ProductOrder> orders = orderService.getAllOrders();

		List<Map<String, Object>> productAnalytics = products.stream().map(product -> {
			// Calculate total sold units (quantity) for each product
			long soldCount = orders.stream().filter(order -> order.getProduct().getId() == product.getId())
					.mapToLong(ProductOrder::getQuantity).sum();

			// Calculate total sales for each product
			double totalSales = orders.stream().filter(order -> order.getProduct().getId() == product.getId())
					.mapToDouble(order -> order.getPrice() * order.getQuantity()).sum();

			// Calculate Average Order Value (AOV)
			double totalOrderValue = orders.stream().filter(order -> order.getProduct().getId() == product.getId())
					.mapToDouble(order -> order.getPrice() * order.getQuantity()).sum();
			long totalOrders = orders.stream().filter(order -> order.getProduct().getId() == product.getId()).count();
			double averageOrderValue = totalOrders > 0 ? totalOrderValue / totalOrders : 0;

			// Calculate the most expensive sale (highest order value for this product)
			double mostExpensiveSale = orders.stream().filter(order -> order.getProduct().getId() == product.getId())
					.mapToDouble(order -> order.getPrice() * order.getQuantity()).max().orElse(0);

			// Calculate average quantity per order
			double averageQuantityPerOrder = totalOrders > 0 ? (double) soldCount / totalOrders : 0;

			// Prepare the analytics data in a map
			Map<String, Object> analytics = new HashMap<>();
			analytics.put("productName", product.getTitle());
			analytics.put("soldCount", soldCount);
			analytics.put("totalSales", totalSales);
			analytics.put("averageOrderValue", averageOrderValue);
			analytics.put("mostExpensiveSale", mostExpensiveSale);
			analytics.put("averageQuantityPerOrder", averageQuantityPerOrder);
			analytics.put("totalOrders", totalOrders);

			return analytics;
		}).collect(Collectors.toList());

		// Add the product analytics data to the model
		model.addAttribute("productAnalytics", productAnalytics);
		return "/admin/product_analytics"; // Your view name
	}

	// read online

	@Autowired
	private ReadOnlineService readOnlineService;

	@GetMapping("/library")
	public String manageLibrary(Model model, @RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "5") int pageSize) {
		Page<ReadOnline> page = readOnlineService.getAllDocuments("", pageNo, pageSize);
		model.addAttribute("documents", page.getContent());
		model.addAttribute("currentPage", pageNo);
		model.addAttribute("totalPages", page.getTotalPages());
		return "admin/library";
	}

	@GetMapping("/add-document")
	public String showAddDocument(Model model) {
		model.addAttribute("document", new ReadOnline());
		// Add categories to the model
		model.addAttribute("categories", categoryService.getAllActiveCategory());
		return "admin/add_document";
	}

	@PostMapping("/save-document")
	public String saveDocument(@RequestParam("title") String title, @RequestParam("description") String description,
			@RequestParam("category") String category, @RequestParam("file") MultipartFile file,
			@RequestParam("coverImage") MultipartFile coverImage, HttpSession session) throws IOException {

		ReadOnline doc = new ReadOnline();
		doc.setTitle(title);
		doc.setDescription(description);
		doc.setCategory(category);

		if (!coverImage.isEmpty()) {
			String coverImageName = saveFile(coverImage, "cover_images");
			doc.setCoverImage(coverImageName);
		}

		if (!file.isEmpty()) {
			String pdfFileName = saveFile(file, "pdf");
			doc.setFile(pdfFileName);
		}

		ReadOnline savedDoc = readOnlineService.saveDocument(doc);

		if (savedDoc != null) {
			session.setAttribute("succMsg", "Document saved successfully");
		} else {
			session.setAttribute("errorMsg", "Failed to save document");
		}

		return "redirect:/admin/library";
	}

	@GetMapping("/delete-document/{id}")
	public String deleteDocument(@PathVariable int id, HttpSession session) {
		readOnlineService.deleteDocument(id);
		session.setAttribute("succMsg", "Document deleted successfully");
		return "redirect:/admin/library";
	}

	@GetMapping("/edit-document/{id}")
	public String showEditDocument(@PathVariable int id, Model model) {
		ReadOnline document = readOnlineService.getDocumentById(id);
		model.addAttribute("document", document);
		model.addAttribute("categories", categoryService.getAllActiveCategory());
		return "admin/edit_library";
	}

	@PostMapping("/update-document")
	public String updateDocument(@RequestParam("id") int id, @RequestParam("coverImage") MultipartFile coverImage,
			@RequestParam("title") String title, @RequestParam("description") String description,
			@RequestParam("category") String category,
			@RequestParam(value = "file", required = false) MultipartFile file, HttpSession session)
			throws IOException {

		ReadOnline existingDoc = readOnlineService.getDocumentById(id);

		// Update fields
		existingDoc.setTitle(title);
		existingDoc.setDescription(description);
		existingDoc.setCategory(category);

		if (!coverImage.isEmpty()) {
			// Delete old cover image
			if (existingDoc.getCoverImage() != null) {
				deleteFile(existingDoc.getCoverImage(), "cover_images");
			}
			String newCoverImage = saveFile(coverImage, "cover_images");
			existingDoc.setCoverImage(newCoverImage);
		}

		if (file != null && !file.isEmpty()) {
			// Delete old file using centralized method
			if (existingDoc.getFile() != null) {
				deleteFile(existingDoc.getFile(), "pdf");
			}
			// Save new file using same method
			String fileName = saveFile(file, "pdf");
			existingDoc.setFile(fileName);
		}

		ReadOnline updatedDoc = readOnlineService.updateDocument(existingDoc);

		if (updatedDoc != null) {
			session.setAttribute("succMsg", "Document updated successfully");
		} else {
			session.setAttribute("errorMsg", "Failed to update document");
		}

		return "redirect:/admin/library";
	}

	private String saveFile(MultipartFile file, String directory) throws IOException {
		// Create directory if not exists
		File uploadDir = new File("uploads/" + directory);
		if (!uploadDir.exists()) {
			uploadDir.mkdirs();
		}

		// Preserve original filename with URL-safe encoding
		String originalFileName = file.getOriginalFilename();
		String safeFileName = originalFileName.replace(" ", "_");
		String fileName = System.currentTimeMillis() + "_" + safeFileName;

		Path path = Paths.get(uploadDir.getAbsolutePath(), fileName);
		Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

		return fileName;
	}

	private void deleteFile(String fileName, String directory) throws IOException {
		File uploadDir = new File("uploads/" + directory);
		Path path = Paths.get(uploadDir.getAbsolutePath(), fileName);

		if (Files.exists(path)) {
			Files.delete(path);
		} else {
//			logger.warn("File not found for deletion: {}", path.toString());
		}
	}

	@GetMapping("/products/export-pdf")
	public void exportProductsToPdf(@RequestParam(name = "ch", required = false) String searchQuery,
			HttpServletResponse response) throws IOException {

		List<Product> products;
		if (searchQuery != null && !searchQuery.trim().isEmpty()) {
			products = productService.searchProduct(searchQuery.trim());
		} else {
			products = productService.getAllProducts();
		}

		response.setContentType("application/pdf");
		response.setHeader("Content-Disposition", "attachment; filename=products_export.pdf");

		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);

			PDPageContentStream contentStream = new PDPageContentStream(document, page);

			// Design parameters
			final float margin = 40;
			float yPosition = page.getMediaBox().getHeight() - margin;
			final float lineHeight = 20;
			final float sectionSpacing = 30;

			// Header Section
			contentStream.setNonStrokingColor(0, 32, 73); // Navy blue
			contentStream.addRect(0, page.getMediaBox().getHeight() - 60, page.getMediaBox().getWidth(), 60);
			contentStream.fill();

			contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
			contentStream.setNonStrokingColor(255, 255, 255); // White text
			contentStream.beginText();
			contentStream.newLineAtOffset(margin, yPosition - 10);
			contentStream.showText("Product Inventory Report");
			contentStream.endText();
			yPosition -= 80;

			// Column headers section - modified
			contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
			contentStream.setNonStrokingColor(0, 32, 73); // Use navy blue from main header

			String[] headers = { "No.", "Product Title", "Category", "Price (Rs)", "Stock" };
			float[] columnWidths = { 40, 220, 120, 100, 50 };
			float availableWidth = page.getMediaBox().getWidth() - 2 * margin;

			// Draw header text (without background)
			float xPosition = margin;
			for (int i = 0; i < headers.length; i++) {
				contentStream.beginText();

				// Center alignment for numeric columns
				if (i == 0 || i >= 3) {
					float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(headers[i]) / 1000 * 10;
					contentStream.newLineAtOffset(xPosition + (columnWidths[i] - textWidth) / 2, yPosition);
				} else {
					contentStream.newLineAtOffset(xPosition + 5, yPosition);
				}

				contentStream.showText(headers[i]);
				contentStream.endText();
				xPosition += columnWidths[i];
			}

			// Add underline instead of background
			contentStream.setLineWidth(1f);
			contentStream.setStrokingColor(0, 32, 73); // Navy blue line
			contentStream.moveTo(margin, yPosition - 5);
			contentStream.lineTo(page.getMediaBox().getWidth() - margin, yPosition - 5);
			contentStream.stroke();

			yPosition -= 25; // Reduced spacing after header

			// Product list
			contentStream.setFont(PDType1Font.HELVETICA, 10);
			int counter = 1;
			for (Product product : products) {
				if (yPosition < margin + 50) { // New page check
					contentStream.close();
					page = new PDPage();
					document.addPage(page);
					contentStream = new PDPageContentStream(document, page);
					yPosition = page.getMediaBox().getHeight() - margin;
					counter = 1; // Reset counter for new page
				}

				// Alternating row background
				if (counter % 2 == 1) {
					contentStream.setNonStrokingColor(250, 250, 250); // Off-white
				} else {
					contentStream.setNonStrokingColor(255, 255, 255); // White
				}
				contentStream.addRect(margin, yPosition - 15, page.getMediaBox().getWidth() - 2 * margin, 20);
				contentStream.fill();

				// Row content
				contentStream.setNonStrokingColor(51, 51, 51); // Dark gray

				// Numbering (01, 02...)
				String formattedCounter = String.format("%02d", counter);
				contentStream.beginText();
				contentStream.newLineAtOffset(margin + 5, yPosition - 10);
				contentStream.showText(formattedCounter);
				contentStream.endText();

				// Product Title
				contentStream.beginText();
				contentStream.newLineAtOffset(margin + 50, yPosition - 10);
				contentStream.showText(product.getTitle());
				contentStream.endText();

				// Category
				contentStream.beginText();
				contentStream.newLineAtOffset(margin + 300, yPosition - 10);
				contentStream.showText(product.getCategory());
				contentStream.endText();

				// Price (green for emphasis)
				contentStream.setNonStrokingColor(0, 128, 0); // Dark green
				contentStream.beginText();
				contentStream.newLineAtOffset(margin + 420, yPosition - 10);
				contentStream.showText("Rs" + product.getPrice());
				contentStream.endText();

				// Stock (gray)
				contentStream.setNonStrokingColor(119, 119, 119); // Medium gray
				contentStream.beginText();
				contentStream.newLineAtOffset(margin + 500, yPosition - 10);
				contentStream.showText(String.valueOf(product.getStock()));
				contentStream.endText();

				// Reset text color
				contentStream.setNonStrokingColor(51, 51, 51);

				// Bottom border
				contentStream.setLineWidth(0.5f);
				contentStream.setStrokingColor(224, 224, 224); // Light gray
				contentStream.moveTo(margin, yPosition - 20);
				contentStream.lineTo(page.getMediaBox().getWidth() - margin, yPosition - 20);
				contentStream.stroke();

				yPosition -= 25;
				counter++;
			}

			// Footer
			contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
			contentStream.setNonStrokingColor(153, 153, 153);
			contentStream.beginText();
			contentStream.newLineAtOffset(margin, margin - 10);
			contentStream.showText("Generated on: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
			contentStream.endText();

			contentStream.close();
			document.save(response.getOutputStream());
		}
	}

	@GetMapping("/users/export-pdf")
	public void exportUsersToPdf(@RequestParam Integer type, HttpServletResponse response) throws IOException {
		List<UserDtls> users = userService.getUsers(type == 1 ? "ROLE_USER" : "ROLE_ADMIN");

		response.setContentType("application/pdf");
		response.setHeader("Content-Disposition", "attachment; filename=users_export.pdf");

		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);

			PDPageContentStream contentStream = new PDPageContentStream(document, page);

			// Design parameters
			final float margin = 40;
			float yPosition = page.getMediaBox().getHeight() - margin;
			final float pageWidth = page.getMediaBox().getWidth();
			final float tableWidth = pageWidth - 2 * margin;

			// Header Section
			contentStream.setNonStrokingColor(0, 32, 73); // Navy blue
			contentStream.addRect(0, page.getMediaBox().getHeight() - 60, pageWidth, 60);
			contentStream.fill();

			// Report title
			contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
			contentStream.setNonStrokingColor(255, 255, 255);
			contentStream.beginText();
			contentStream.newLineAtOffset(margin, yPosition - 10);
			contentStream.showText(type == 1 ? "User Management Report" : "Admin Management Report");
			contentStream.endText();
			yPosition -= 80;

			// Column configuration
			String[] headers = { "Name", "Email", "Phone", "Status" };
			float[] colWidths = { 120, 220, 100, 80 };
			float[] colPositions = new float[4];
			colPositions[0] = margin;
			for (int i = 1; i < colWidths.length; i++) {
				colPositions[i] = colPositions[i - 1] + colWidths[i - 1];
			}

			// Draw column headers
			contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
			contentStream.setNonStrokingColor(0, 32, 73);
			for (int i = 0; i < headers.length; i++) {
				contentStream.beginText();
				float xOffset = colPositions[i] + (i == 3
						? (colWidths[i] - PDType1Font.HELVETICA_BOLD.getStringWidth(headers[i]) / 1000 * 10) / 2
						: 5);
				contentStream.newLineAtOffset(xOffset, yPosition);
				contentStream.showText(headers[i]);
				contentStream.endText();
			}

			// Header underline
			contentStream.setLineWidth(1f);
			contentStream.setStrokingColor(0, 32, 73);
			contentStream.moveTo(margin, yPosition - 8);
			contentStream.lineTo(pageWidth - margin, yPosition - 8);
			contentStream.stroke();
			yPosition -= 25;

			// User data
			contentStream.setFont(PDType1Font.HELVETICA, 10);
			int rowCounter = 1;
			for (UserDtls user : users) {
				if (yPosition < margin + 50) { // Page break
					contentStream.close();
					page = new PDPage();
					document.addPage(page);
					contentStream = new PDPageContentStream(document, page);
					yPosition = page.getMediaBox().getHeight() - margin;
					rowCounter = 1;
				}

				// Alternating row background
				contentStream.setNonStrokingColor(rowCounter % 2 == 1 ? new Color(245, 245, 245) : Color.WHITE);
				contentStream.addRect(margin, yPosition - 15, tableWidth, 20);
				contentStream.fill();

				// Row content
				contentStream.setNonStrokingColor(51, 51, 51);

				// Name
				String name = truncateText(user.getName(), 20);
				contentStream.beginText();
				contentStream.newLineAtOffset(colPositions[0] + 5, yPosition - 10);
				contentStream.showText(name);
				contentStream.endText();

				// Email
				contentStream.beginText();
				contentStream.newLineAtOffset(colPositions[1] + 5, yPosition - 10);
				contentStream.showText(truncateText(user.getEmail(), 35));
				contentStream.endText();

				// Phone
				String phone = user.getMobileNumber() != null ? user.getMobileNumber() : "-";
				contentStream.beginText();
				contentStream.newLineAtOffset(colPositions[2] + 5, yPosition - 10);
				contentStream.showText(phone);
				contentStream.endText();

				// Status
				String status = user.getIsEnable() ? "Active" : "Inactive";
				contentStream.setNonStrokingColor(user.getIsEnable() ? new Color(0, 128, 0) : new Color(220, 53, 69));
				contentStream.beginText();
				float statusX = colPositions[3]
						+ (colWidths[3] - PDType1Font.HELVETICA.getStringWidth(status) / 1000 * 10) / 2;
				contentStream.newLineAtOffset(statusX, yPosition - 10);
				contentStream.showText(status);
				contentStream.endText();

				yPosition -= 25;
				rowCounter++;
			}

			// Footer (Fixed date/time formatting)
			contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
			contentStream.setNonStrokingColor(153, 153, 153);
			contentStream.beginText();
			contentStream.newLineAtOffset(margin, margin - 10);
			contentStream.showText(
					"Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
			contentStream.endText();

			contentStream.close();
			document.save(response.getOutputStream());
		}
	}

	private String truncateText(String text, int maxLength) {
		return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
	}

	@GetMapping("/documents/export-pdf")
	public void exportDocumentsToPdf(HttpServletResponse response) throws IOException {
		List<ReadOnline> documents = readOnlineService.getAllDocuments();

		response.setContentType("application/pdf");
		response.setHeader("Content-Disposition", "attachment; filename=documents_export.pdf");

		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage();
			document.addPage(page);

			PDPageContentStream contentStream = new PDPageContentStream(document, page);

			// Design parameters
			final float margin = 40;
			float yPosition = page.getMediaBox().getHeight() - margin;
			final float pageWidth = page.getMediaBox().getWidth();
			final float tableWidth = pageWidth - 2 * margin;

			// Header Section
			contentStream.setNonStrokingColor(0, 32, 73); // Navy blue
			contentStream.addRect(0, page.getMediaBox().getHeight() - 60, pageWidth, 60);
			contentStream.fill();

			// Report title
			contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
			contentStream.setNonStrokingColor(255, 255, 255);
			contentStream.beginText();
			contentStream.newLineAtOffset(margin, yPosition - 10);
			contentStream.showText("Document Management Report");
			contentStream.endText();
			yPosition -= 80;

			// Column configuration
			String[] headers = { "#", "Title", "Category", "Description" };
			float[] colWidths = { 40, 180, 140, 250 }; // Adjusted widths
			float[] colPositions = new float[4];
			colPositions[0] = margin;
			for (int i = 1; i < colWidths.length; i++) {
				colPositions[i] = colPositions[i - 1] + colWidths[i - 1];
			}

			// Draw column headers
			contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
			contentStream.setNonStrokingColor(0, 32, 73);
			for (int i = 0; i < headers.length; i++) {
				contentStream.beginText();
				float xOffset = colPositions[i] + 8; // Uniform padding
				if (i == 0) { // Center align the number column
					float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(headers[i]) / 1000 * 10;
					xOffset = colPositions[i] + (colWidths[i] - textWidth) / 2;
				}
				contentStream.newLineAtOffset(xOffset, yPosition);
				contentStream.showText(headers[i]);
				contentStream.endText();
			}

			// Header underline
			contentStream.setLineWidth(1f);
			contentStream.setStrokingColor(200, 200, 200); // Light gray
			contentStream.moveTo(margin, yPosition - 10);
			contentStream.lineTo(pageWidth - margin, yPosition - 10);
			contentStream.stroke();
			yPosition -= 25;

			// Document data
			contentStream.setFont(PDType1Font.HELVETICA, 10);
			int rowCounter = 1;
			for (ReadOnline doc : documents) {
				if (yPosition < margin + 50) { // Page break
					contentStream.close();
					page = new PDPage();
					document.addPage(page);
					contentStream = new PDPageContentStream(document, page);
					yPosition = page.getMediaBox().getHeight() - margin;
					rowCounter = 1;
				}

				// Alternating row background
				contentStream.setNonStrokingColor(rowCounter % 2 == 1 ? new Color(250, 250, 250) : Color.WHITE);
				contentStream.addRect(margin, yPosition - 15, tableWidth, 20);
				contentStream.fill();

				// Row content
				contentStream.setNonStrokingColor(51, 51, 51);

				// Document Number (centered)
				contentStream.beginText();
				String counter = String.valueOf(rowCounter);
				float numWidth = PDType1Font.HELVETICA.getStringWidth(counter) / 1000 * 10;
				contentStream.newLineAtOffset(colPositions[0] + (colWidths[0] - numWidth) / 2, yPosition - 10);
				contentStream.showText(counter);
				contentStream.endText();

				// Title (left-aligned)
				String title = truncatedocText(doc.getTitle(), 30);
				contentStream.beginText();
				contentStream.newLineAtOffset(colPositions[1] + 8, yPosition - 10);
				contentStream.showText(title);
				contentStream.endText();

				// Category (left-aligned)
				contentStream.beginText();
				contentStream.newLineAtOffset(colPositions[2] + 8, yPosition - 10);
				contentStream.showText(doc.getCategory());
				contentStream.endText();

				// Description (left-aligned with ellipsis)
				String description = truncatedocText(doc.getDescription(), 50);
				contentStream.beginText();
				contentStream.newLineAtOffset(colPositions[3] + 8, yPosition - 10);
				contentStream.showText(description);
				contentStream.endText();

				// Row separator
				contentStream.setLineWidth(0.5f);
				contentStream.setStrokingColor(220, 220, 220);
				contentStream.moveTo(margin, yPosition - 20);
				contentStream.lineTo(pageWidth - margin, yPosition - 20);
				contentStream.stroke();

				yPosition -= 25;
				rowCounter++;
			}

			// Footer
			contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
			contentStream.setNonStrokingColor(153, 153, 153);
			contentStream.beginText();
			contentStream.newLineAtOffset(margin, margin - 10);
			contentStream.showText(
					"Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
			contentStream.endText();

			contentStream.close();
			document.save(response.getOutputStream());
		}
	}

	private String truncatedocText(String text, int maxLength) {
		if (text == null || text.isEmpty()) {
			return "-";
		}
		return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
	}
}
