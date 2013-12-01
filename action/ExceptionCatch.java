package action;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import start.Go;

public class ExceptionCatch {
	
	public static boolean catchException(Document doc) throws Exception{
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		try {
			String errorCode = xpath.evaluate("/response/header/error/code", doc);
			String errorMessage = xpath.evaluate("/response/header/error/message", doc);
			if (!errorCode.equals("0")) {
				if(errorCode.equals("9000")){
					throw new Exception(errorCode+"："+errorMessage.replace("\n", "，"));
				}
				Go.log("Error "+errorCode+", error type: "+errorMessage.replace("\n", "，"));
				return true;
			}
		} catch (XPathExpressionException e) {
			Go.log("Error arsing XML");
			return true;
		}
		return false;
	}
	
}
