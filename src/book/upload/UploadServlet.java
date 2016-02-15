package book.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class UploadServlet extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		request.setCharacterEncoding("GBK");
		response.setCharacterEncoding("GBK");
		response.setContentType("text/html; charset=GBK");
		// 创建HttpSession对象
		HttpSession session = request.getSession();

		if ("status".equals(request.getParameter("c"))) {// 如果请求中c的值为status
			doStatus(session, response);// 调用doStatus方法
		} else {// 否则，调用doFileUpload方法
			doFileUpload(session, request, response);
		}
	}
	private void doFileUpload(HttpSession session, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		try {
			// 创建UploadListener对象
			UploadListener listener = new UploadListener(request
					.getContentLength());
			listener.start();// 启动监听状态
			// 将监听器对象的状态保存在Session中
			session.setAttribute("FILE_UPLOAD_STATS", listener
					.getFileUploadStats());
			session.setAttribute("bytesRead","0");
			// 创建MonitoredDiskFileItemFactory对象
			FileItemFactory factory = new MonitoredDiskFileItemFactory(listener);
			// 通过该工厂对象创建ServletFileUpload对象
			ServletFileUpload upload = new ServletFileUpload(factory);
			// 将转化请求保存到list对象中			
			List items = upload.parseRequest(request);
			
			// 停止使用监听器
			listener.done();
			
			boolean hasError = false;
			// 循环list中的对象
			for (Iterator i = items.iterator(); i.hasNext();) {
				FileItem fileItem = (FileItem) i.next();
				if (!fileItem.isFormField()) {// 如果该FileItem不是表单域
					processUploadedFile(fileItem,session);// 调用processUploadedFile方法,将数据保存到文件中
					fileItem.delete();// 内存中删除该数据流
				}
			}

			if (!hasError) {// 如果没有出现错误
				sendCompleteResponse(response, null);// 调用sendCompleteResponse方法
			} else {
				sendCompleteResponse(response,
						"Could not process uploaded file. Please see log for details.");
			}
		} catch (Exception e) {
			sendCompleteResponse(response, e.getMessage());
		}
	}
	public void processUploadedFile(FileItem item,HttpSession session) {
		// 获得上传文件的文件名
		String fileName = item.getName().substring(
				item.getName().lastIndexOf("\\") + 1);
		// 创建File对象,将上传得文件保存到C:\\upload文件夹下
		File file = new File("C:\\upload\\", fileName);
		InputStream in;
		try {
			in = item.getInputStream();// 获得输入数据流文件
			// 将该数据流写入到指定文件中
			FileOutputStream out = new FileOutputStream(file);
			byte[] buffer = new byte[4096]; // To hold file contents
			int bytes_read;
			long bytesRead = Long.parseLong((String) session.getAttribute("bytesRead"));// 已读数据大小
			while ((bytes_read = in.read(buffer)) != -1) // Read until EOF
			{
				out.write(buffer, 0, bytes_read);
				bytesRead+=(long)bytes_read;
				session.setAttribute("bytesRead",String.valueOf(bytesRead));
			}
			UploadListener.FileUploadStats fileUploadStats = (UploadListener.FileUploadStats) session
			.getAttribute("FILE_UPLOAD_STATS");
			long sizeTotal = fileUploadStats.getTotalSize();// 获得上传文件的总大小
			session.setAttribute("bytesRead",String.valueOf(sizeTotal));
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					;
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					;
				}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void doStatus(HttpSession session, HttpServletResponse response)
			throws IOException {
		// 设置该响应不在缓存中读取
		response.addHeader("Expires", "0");
		response.addHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response.addHeader("Cache-Control", "post-check=0, pre-check=0");
		response.addHeader("Pragma", "no-cache");
		// 获得保存在Session中的状态信息
		UploadListener.FileUploadStats fileUploadStats = (UploadListener.FileUploadStats) session
				.getAttribute("FILE_UPLOAD_STATS");
		if (fileUploadStats != null) {
			//long +fileUploadStats.getBytesRead();
			//System.out.println(bytesProcessed);
			long bytesProcessed = fileUploadStats.getBytesRead();// 获得已经上传的数据大小
			long sizeTotal = fileUploadStats.getTotalSize();// 获得上传文件的总大小
			// 计算上传完成的百分比
			long percentComplete = (long) Math
					.floor(((double) bytesProcessed / (double) sizeTotal) * 100.0);
			// 获得上传已用的时间
			long timeInSeconds = fileUploadStats.getElapsedTimeInSeconds();
			// 计算平均上传速率
			double uploadRate = bytesProcessed / (timeInSeconds + 0.00001);
			// 计算总共所需时间
			double estimatedRuntime = sizeTotal / (uploadRate + 0.00001);
			// 将上传状态返回给客户端
			response.getWriter().println("<b>上传状态:</b><br/>");
			if (bytesProcessed != sizeTotal) {
				response.getWriter().println(
						"<div class=\"prog-border\"><div class=\"prog-bar\" style=\"width: "
								+ percentComplete + "%;\"></div></div>");
				response.getWriter().println(
						"完成: " + bytesProcessed + " 总大小： " + sizeTotal
								+ " bytes (" + percentComplete + "%) "
								+ (long) Math.round(uploadRate / 1024)
								+ " Kbs <br/>");
				response.getWriter().println(
						"用时: " + formatTime(timeInSeconds) + " 总时间： "
								+ formatTime(estimatedRuntime) + " 剩余时间："
								+ formatTime(estimatedRuntime - timeInSeconds)
								+ "<br/>");
			} else {
				response.getWriter().println(
						"完成: " + bytesProcessed + " 总大小： " + sizeTotal
								+ " bytes<br/>");
				response.getWriter().println("上传完成.<br/>");
			}
			//如果文件已经上传完毕
			if (fileUploadStats.getBytesRead() == fileUploadStats
							.getTotalSize()) {
				response.getWriter().println("<b>上传完成,正在保存文件... ...</b><br/>");
				//写文件
				bytesProcessed =Long.parseLong((String) session.getAttribute("bytesRead"));
				percentComplete = (long) Math
				.floor(((double) bytesProcessed / (double) sizeTotal) * 100.0);
				if (bytesProcessed != sizeTotal) {
					response.getWriter().println(
							"<div class=\"prog-border\"><div class=\"prog-bar\" style=\"width: "
							+ percentComplete + "%;\"></div></div>");
					response.getWriter().println(
							"保存: " + bytesProcessed + " 总大小： " + sizeTotal
							+ " bytes (" + percentComplete + "%) <br/>");
				} else {
					response.getWriter().println(
							"保存: " + bytesProcessed + " 总大小： " + sizeTotal
							+ " bytes<br/>");
					response.getWriter().println("保存完成.<br/>");
				}
			}
		}	
		
	}

	private void sendCompleteResponse(HttpServletResponse response,
			String message) throws IOException {
		if (message == null) {
			response
					.getOutputStream()
					.print(
							"<html><head><script type='text/javascript'>function killUpdate() { window.parent.killUpdate(''); }</script></head><body onload='killUpdate()'></body></html>");
		} else {
			response
					.getOutputStream()
					.print(
							"<html><head><script type='text/javascript'>function killUpdate() { window.parent.killUpdate('"
									+ message
									+ "'); }</script></head><body onload='killUpdate()'></body></html>");
		}
	}

	private String formatTime(double timeInSeconds) {
		long seconds = (long) Math.floor(timeInSeconds);
		long minutes = (long) Math.floor(timeInSeconds / 60.0);
		long hours = (long) Math.floor(minutes / 60.0);

		if (hours != 0) {
			return hours + "hours " + (minutes % 60) + "minutes "
					+ (seconds % 60) + "seconds";
		} else if (minutes % 60 != 0) {
			return (minutes % 60) + "minutes " + (seconds % 60) + "seconds";
		} else {
			return (seconds % 60) + " seconds";
		}
	}
}
