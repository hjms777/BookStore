package com.book.store.util;

import java.io.File;
import java.util.Calendar;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.book.store.dto.BookDTO;

public class FileManager {
	
	public static void doFileUpload(BookDTO dto,@RequestParam(value = "upload",required = false) MultipartFile[] upload) throws Exception{
		
		for (MultipartFile file : upload) {

			if(!file.isEmpty()) {

				File f = new File("C:/sts-bundle/work/BookStore/src/main/resources/static/image");

				if(!f.exists()) {

					f.mkdirs();

				}

				String newFileName = String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", Calendar.getInstance());
				newFileName+= System.nanoTime();
				newFileName+= file.getOriginalFilename();

				dto.setImage_Url("image" + f.separator + newFileName);
				//img url링크 이걸로 set

				f = new File(newFileName);

				file.transferTo(f);
				//여기서 실제 존재하는 파일로 되는 것임

			}

		}
		
	}

}
