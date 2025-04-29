package com.ecom.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.UserService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Say;
import com.twilio.type.PhoneNumber;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CommonUtil {

	@Value("${twilio.account_sid}")
	private String twilioAccountSid;

	@Value("${twilio.auth.token}")
	private String twilioAuthToken;

	@Value("${twilio.mobile_number}")
	private String twilioFromNumber;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private UserService userService;

	public Boolean sendMail(String url, String recipientEmail, String recieverName)
			throws UnsupportedEncodingException, MessagingException {

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("chamidukeshikaz@gmail.com", "Book-Mart Support Team");
		helper.setTo(recipientEmail);

		String content = "<html>" + "<head>" + "<style>"
				+ "    body { font-family: Arial, sans-serif; color: #333333; }"
				+ "    .email-header { background-color: #4CAF50; color: white; padding: 10px 20px; text-align: center; }"
				+ "    .email-body { margin: 20px; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px; }"
				+ "    .email-footer { margin-top: 20px; font-size: 12px; color: #888888; text-align: center; }"
				+ "    a { color: #4CAF50; text-decoration: none; font-weight: bold; }" + "</style>" + "</head>"
				+ "<body>" + "    <div class=\"email-header\">" + "        <h1>Password Reset Request</h1>"
				+ "    </div>" + "    <div class=\"email-body\">" + "        <p>Dear " + recieverName + ",</p>"
				+ "        <p>We received a request to reset your password for your <strong>Book-Mart</strong> account. If you made this request, you can reset your password by clicking the button below:</p>"
				+ "        <p style=\"text-align: center;\">" + "            <a href=\"" + url
				+ "\" style=\"background-color: #4CAF50; color: white; padding: 10px 20px; border-radius: 5px; display: inline-block;\">Reset Password</a>"
				+ "        </p>"
				+ "        <p>If you did not request to reset your password, please ignore this email. Your account is safe, and no changes have been made.</p>"
				+ "        <p>For any assistance, please contact our support team at <a href=\"mailto:support@bookmart.com\">support@bookmart.com</a>.</p>"
				+ "        <p>Thank you,<br>The Book-Mart App Team</p>" + "    </div>"
				+ "    <div class=\"email-footer\">"
				+ "        <p>This email was sent to you because a password reset request was initiated for your account. If you believe this was a mistake, please disregard this email.</p>"
				+ "        <p>&copy; 2025 Book-Mart App. All rights reserved.</p>" + "    </div>" + "</body>"
				+ "</html>";

		helper.setSubject("Reset Your Book-Mart Password");
		helper.setText(content, true);
		mailSender.send(message);

		return true;
	}

	public static String generateUrl(HttpServletRequest request) {

		// http://localhost:8080/forgot-password
		String siteUrl = request.getRequestURL().toString();

		return siteUrl.replace(request.getServletPath(), "");

	}

	String msg = null;

	public Boolean sendMailForProductOrder(ProductOrder order, String status) throws Exception {

		msg = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; padding: 20px; background-color: #f4f4f9;\">"
				+ "<div style=\"max-width: 600px; margin: auto; background: #ffffff; box-shadow: 0 4px 8px rgba(0,0,0,0.1); border-radius: 10px; overflow: hidden;\">"
				+ "<div style=\"padding: 20px; background-color: #5b9bd5; color: white; text-align: center;\">"
				+ "    <h2 style=\"margin: 0; font-size: 24px;\">Order Status Update</h2>" + "</div>"
				+ "<div style=\"padding: 20px;\">"
				+ "    <p style=\"font-size: 18px;\">Dear <strong>[[name]]</strong>,</p>"
				+ "    <p>We are delighted to share the status of your recent order:</p><p>ID : [[orderId]]</p>"
				+ "    <div style=\"background: #e7f5e9; padding: 20px; border-radius: 8px; margin-top: 20px;\">"
				+ "        <div style=\"margin-bottom: 10px; color: #4caf50; font-size: 16px;\">"
				+ "            <strong>Status:</strong> [[orderStatus]]" + "        </div>"
				+ "        <div style=\"margin-bottom: 10px;\">"
				+ "            <strong>Product Name:</strong> [[productName]]" + "        </div>"
				+ "        <div style=\"margin-bottom: 10px; background-color: #f9f9f9; padding: 10px; border-radius: 6px;\">"
				+ "            <strong>Category:</strong> [[category]]" + "        </div>"
				+ "        <div style=\"margin-bottom: 10px;\">" + "            <strong>Quantity:</strong> [[quantity]]"
				+ "        </div>"
				+ "        <div style=\"margin-bottom: 10px; background-color: #f9f9f9; padding: 10px; border-radius: 6px;\">"
				+ "            <strong>Total Order Cost:</strong> [[price]]" + "        </div>"
				+ "        <div style=\"margin-bottom: 10px;\">"
				+ "            <strong>Payment Type:</strong> [[paymentType]]" + "        </div>" + "    </div>"
				+ "    <p style=\"margin-top: 20px;\">If you have any questions, feel free to contact our support team. We're here to help!</p>"
				+ "    <p>Thank you for choosing us. We appreciate your business!</p>"
				+ "    <div style=\"text-align: center; margin-top: 20px;\">"
				+ "        <a href=\"https://support.bookmart.com\" style=\"display: inline-block; background: #5b9bd5; color: white; text-decoration: none; padding: 10px 20px; border-radius: 5px;\">Contact Support</a>"
				+ "    </div>" + "</div>"
				+ "<div style=\"padding: 10px; text-align: center; background: #f1f1f1; color: #888; font-size: 14px;\">"
				+ "    Book-Mart App Support Team &copy; 2025" + "</div>" + "</div>" + "</div>";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true); // Set true for multipart email

		helper.setFrom("chamidukeshikaz@gmail.com", "Book-Mart App Support Team");
		helper.setTo(order.getOrderAddress().getEmail());

		Double Total = (order.getPrice()) * (order.getQuantity());

		// Replace placeholders with actual values
		msg = msg.replace("[[name]]", order.getOrderAddress().getFirstName());
		msg = msg.replace("[[orderId]]", order.getOrderId());
		msg = msg.replace("[[orderStatus]]", status);
		msg = msg.replace("[[productName]]", order.getProduct().getTitle());
		msg = msg.replace("[[category]]", order.getProduct().getCategory());
		msg = msg.replace("[[quantity]]", order.getQuantity().toString());
		msg = msg.replace("[[price]]", Total.toString());
		msg = msg.replace("[[paymentType]]", order.getPaymentType());

		helper.setSubject("Product Order Status");
		helper.setText(msg, true); // true for HTML content
		mailSender.send(message);

		return true;
	}

	public UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

	public Boolean sendMailForAllProductOrders(List<ProductOrder> orders, String status) throws Exception {
		StringBuilder msgBuilder = new StringBuilder();

		msgBuilder.append(
				"<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; padding: 20px; background-color: #f4f4f9;\">")
				.append("<div style=\"max-width: 600px; margin: auto; background: #ffffff; box-shadow: 0 4px 8px rgba(0,0,0,0.1); border-radius: 10px; overflow: hidden;\">")
				.append("<div style=\"padding: 20px; background-color: #5b9bd5; color: white; text-align: center;\">")
				.append("<h2 style=\"margin: 0; font-size: 24px;\">Order Status Update</h2></div>")
				.append("<div style=\"padding: 20px;\">")
				.append("<p style=\"font-size: 18px;\">Dear <strong>[[name]]</strong>,</p>")
				.append("<p>We are delighted to share the status of your recent order:</p>")
				.append("<p><strong>Order Status:</strong> ").append(status).append("</p>");

		// Order Summary Section
		msgBuilder.append("<div style=\"background: #e7f5e9; padding: 20px; border-radius: 8px; margin-top: 20px;\">")
				.append("<h3 style=\"margin-top: 0; color: #4caf50;\">Order Summary</h3>")
				.append("<table style=\"width: 100%; border-collapse: collapse; margin-bottom: 15px;\">")
				.append("<thead><tr>")
				.append("<th style=\"padding: 10px; background-color: #f9f9f9; border-bottom: 2px solid #ddd; text-align: left;\">Product</th>")
				.append("<th style=\"padding: 10px; background-color: #f9f9f9; border-bottom: 2px solid #ddd; text-align: left;\">Category</th>")
				.append("<th style=\"padding: 10px; background-color: #f9f9f9; border-bottom: 2px solid #ddd; text-align: left;\">Quantity</th>")
				.append("<th style=\"padding: 10px; background-color: #f9f9f9; border-bottom: 2px solid #ddd; text-align: left;\">Price</th>")
				.append("<th style=\"padding: 10px; background-color: #f9f9f9; border-bottom: 2px solid #ddd; text-align: left;\">Subtotal</th>")
				.append("</tr></thead><tbody>");

		double grandTotal = 0.0;
		String paymentType = orders.get(0).getPaymentType();

		for (ProductOrder order : orders) {
			double subtotal = order.getPrice() * order.getQuantity();
			grandTotal += subtotal;

			msgBuilder.append("<tr>").append("<td style=\"padding: 10px; border-bottom: 1px solid #ddd;\">")
					.append(order.getProduct().getTitle()).append("</td>")
					.append("<td style=\"padding: 10px; border-bottom: 1px solid #ddd;\">")
					.append(order.getProduct().getCategory()).append("</td>")
					.append("<td style=\"padding: 10px; border-bottom: 1px solid #ddd;\">").append(order.getQuantity())
					.append("</td>").append("<td style=\"padding: 10px; border-bottom: 1px solid #ddd;\">Rs ")
					.append(String.format("%.2f", order.getPrice())).append("</td>")
					.append("<td style=\"padding: 10px; border-bottom: 1px solid #ddd;\">Rs ")
					.append(String.format("%.2f", subtotal)).append("</td>").append("</tr>");
		}

		msgBuilder.append("</tbody></table>").append(
				"<div style=\"margin-top: 15px; padding: 15px; background-color: #f9f9f9; border-radius: 6px;\">")
				.append("<div style=\"font-weight: bold; margin-bottom: 10px;\">Total Amount: Rs ")
				.append(String.format("%.2f", grandTotal)).append("</div>")
				.append("<div style=\"font-weight: bold;\">Payment Method: ").append(paymentType).append("</div>")
				.append("</div></div>");

		// Closing content
		msgBuilder.append(
				"<p style=\"margin-top: 20px;\">If you have any questions, feel free to contact our support team. We're here to help!</p>")
				.append("<p>Thank you for choosing us. We appreciate your business!</p>")
				.append("<div style=\"text-align: center; margin-top: 20px;\">")
				.append("<a href=\"https://support.bookmart.com\" style=\"display: inline-block; background: #5b9bd5; color: white; text-decoration: none; padding: 10px 20px; border-radius: 5px;\">Contact Support</a>")
				.append("</div></div>")
				.append("<div style=\"padding: 10px; text-align: center; background: #f1f1f1; color: #888; font-size: 14px;\">")
				.append("Book-Mart App Support Team &copy; 2025</div></div></div>");

		String msg = msgBuilder.toString();

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);

		helper.setFrom("chamidukeshikaz@gmail.com", "Book-Mart App Support Team");
		helper.setTo(orders.get(0).getOrderAddress().getEmail());

		msg = msg.replace("[[name]]", orders.get(0).getOrderAddress().getFirstName());

		helper.setSubject("Order Summary and Status");
		helper.setText(msg, true);
		mailSender.send(message);

		return true;
	}

	public void sendLowStockEmail(Product product) {
		try {
			String adminEmail = "chamidukeshikaz@gmail.com";
			String subject = "LOW STOCK ALERT: " + product.getTitle();

			String content = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; padding: 20px; background-color: #f4f4f9;\">"
					+ "<div style=\"max-width: 600px; margin: auto; background: #ffffff; box-shadow: 0 4px 8px rgba(0,0,0,0.1); border-radius: 10px; overflow: hidden;\">"
					+ "<div style=\"padding: 20px; background-color: #ff9900; color: white; text-align: center;\">"
					+ "<h2 style=\"margin: 0; font-size: 24px;\">Low Stock Notification</h2></div>"
					+ "<div style=\"padding: 20px;\">" + "<p>Hello Admin,</p>"
					+ "<p>The following product needs restocking:</p>"
					+ "<div style=\"background: #fff3cd; padding: 15px; border-radius: 8px; margin: 15px 0;\">"
					+ "<p><strong>Product:</strong> " + product.getTitle() + "</p>" + "<p><strong>Category:</strong> "
					+ product.getCategory() + "</p>" + "<p><strong>Current Stock:</strong> " + product.getStock()
					+ "</p>" + "</div>" + "<p>Please restock this item to maintain inventory levels.</p>" + "</div>"
					+ "<div style=\"padding: 10px; text-align: center; background: #f1f1f1; color: #888; font-size: 14px;\">"
					+ "Automated Inventory Alert - Book-Mart System</div></div></div>";

			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setFrom("chamidukeshikaz@gmail.com", "Book-Mart Inventory System");
			helper.setTo(adminEmail);
			helper.setSubject(subject);
			helper.setText(content, true);
			mailSender.send(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendLowStockSms(Product product) {
		try {
			Twilio.init(twilioAccountSid, twilioAuthToken);

			String toNumber = "+94788889853";
			String messageBody = String.format(
					"⚠️ Low Stock Alert! ⚠️\nProduct: %s\nCategory: %s\nCurrent Stock: %d\nAction Required: Please restock immediately!",
					product.getTitle(), product.getCategory(), product.getStock());

			Message.creator(new PhoneNumber(toNumber), new PhoneNumber(twilioFromNumber), messageBody).create();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendLowStockVoiceAlert(Product product) {
		try {
			Twilio.init(twilioAccountSid, twilioAuthToken);

			String messageBody = "Low Stock Alert! " + product.getTitle() + " in category " + product.getCategory()
					+ ". Current stock is " + product.getStock() + ". Please restock immediately.";

			VoiceResponse response = new VoiceResponse.Builder().say(new Say.Builder(messageBody).build()).build();

			String twimlUrl = "https://handler.twilio.com/twiml/EH38081c8592366a2f6a29a14f028dcb65";

			Call call = Call
					.creator(new PhoneNumber("+94788889853"), new PhoneNumber(twilioFromNumber), URI.create(twimlUrl))
					.create();

			System.out.println("Voice call initiated: " + call.getSid());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}