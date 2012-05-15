package ch.rasc.upload.web;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class UploadController {

	@Autowired
	private FileManager fileManager;

	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public void chunkExists(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("resumableChunkNumber") Integer chunkNumber, @RequestParam("resumableChunkSize") Long chunkSize,
			@RequestParam("resumableIdentifier") String identifier, @RequestParam("resumableFilename") String filename) throws IOException {

		if (fileManager.chunkExists(identifier, chunkNumber, chunkSize)) {
			// do not upload chunk again
			response.setStatus(200);
		} else {
			// chunk not on the server, upload it
			response.setStatus(404);
		}
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public void processUpload(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "resumableChunkNumber") Integer chunkNumber, @RequestParam(value = "resumableChunkSize") Long chunkSize,
			@RequestParam(value = "resumableTotalSize") Long totalSize, @RequestParam(value = "resumableIdentifier") String identifier,
			@RequestParam(value = "resumableFilename") String fileName, @RequestParam(value = "file") MultipartFile file)
			throws IOException {

		if (!fileManager.isSupported(fileName)) {
			// cancel the whole upload
			response.setStatus(501);
			return;
		}

		try (InputStream is = file.getInputStream()) {
			fileManager.storeChunk(identifier, chunkNumber, is);
		}

		if (fileManager.allChunksUploaded(identifier, chunkSize, totalSize)) {
			fileManager.mergeAndDeleteChunks(fileName, identifier, chunkSize, totalSize);
		}

		response.setStatus(200);

	}

	@RequestMapping(value = "/simpleUpload", method = RequestMethod.POST)
	public String processUpload(HttpServletRequest request) throws IOException, ServletException {

		for (Part part : request.getParts()) {

			for (String header : part.getHeaderNames()) {
				System.out.println(header);
			}

			System.out.println("FILENAME   : " + getFileName(part));
			System.out.println("CONTENTTYPE: " + part.getContentType());
			System.out.println("SIZE       : " + part.getSize());
			System.out.println();
			part.delete();
		}
		return "redirect:index2.html";

	}

	private String getFileName(Part part) {
		String partHeader = part.getHeader("content-disposition");

		for (String cd : partHeader.split(";")) {
			if (cd.trim().startsWith("filename")) {
				return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}

}
