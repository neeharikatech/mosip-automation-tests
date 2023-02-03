package io.mosip.ivv.orchestrator;

import static io.restassured.RestAssured.given;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.MediaType;

import org.testng.Reporter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.mosip.admin.fw.util.AdminTestUtil;
import io.mosip.admin.fw.util.TestCaseDTO;
import io.mosip.authentication.fw.precon.JsonPrecondtion;
import io.mosip.authentication.fw.util.RestClient;
import io.mosip.ivv.core.base.BaseStep;
import io.mosip.ivv.e2e.constant.E2EConstants;
import io.mosip.service.BaseTestCase;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class BaseTestCaseUtil extends BaseStep{
	public static HashMap<String, String> pridsAndRids=new LinkedHashMap<String, String>();
	public static HashMap<String, String> uinReqIds = new LinkedHashMap<String, String>();
	public static Properties props = new AdminTestUtil().getproperty(TestRunner.getExternalResourcePath()+"/config/test-orchestrator_mz.properties");
	public String baseUrl=props.getProperty("packetUtilityBaseUrl");
	public static HashMap<String, String> residentTemplatePaths = new LinkedHashMap<String, String>();
	public static HashMap<String, String> residentPathsPrid = new LinkedHashMap<String, String>();
	public static HashMap<String, String> templatePacketPath = new LinkedHashMap<String, String>();
	public static HashMap<String, String> manualVerificationRid = new LinkedHashMap<String, String>();
	public static HashMap<String, String> residentPathGuardianRid = null;
	public static final long DEFAULT_WAIT_TIME = 30000l;
	public static final long TIME_IN_MILLISEC = 1000l;
	public static String prid=null;
	public static String statusCode=null;
	public static PacketUtility packetUtility= new PacketUtility();
	public static HashMap<String, String> contextKey=new HashMap<String, String>();
	public static HashMap<String, String> contextInuse=new HashMap<String, String>();
	public static List<String> resDataPathList= new LinkedList();
	public static Properties uinPersonaProp=new Properties();
	public static Properties vidPersonaProp=new Properties();
	public static Properties oidcClientProp=new Properties();
	public static Properties oidcPmsProp=new Properties();
	public static HashMap<String, String> ridPersonaPath=new LinkedHashMap<String, String>();
	public static Properties residentPersonaIdPro=new Properties();
	public static Properties ridPacketPathPro=new Properties();
	public static Hashtable<String,Map<String,String>> hashtable= new Hashtable<>();
	public static List<String> generatedResidentData =new ArrayList<>();
	public static String templatPath_updateResident=null;
	public static String rid_updateResident=null;
	public static String uin_updateResident=null;
	public static String prid_updateResident=null;
	public static String scenario = null;
	public static String partnerKeyUrl = null;
	public static String partnerId = null;
	public BaseTestCaseUtil() {}
	
	public String getDateTime() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMddHHmmssSSS");
		LocalDateTime now = LocalDateTime.now();
		return "DSL"+dtf.format(now);
	}
	public String getDateTimePrint() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-mmm-yyyy hh:mm:ss.s");
		LocalDateTime now = LocalDateTime.now();
		return "DSL Time: "+dtf.format(now);
	}
	
	public String encoder(String text) {
		return Base64.getEncoder().encodeToString(text.getBytes());
	}
	
	public String readProperty() {
		String preRegistrationId=null;
		FileInputStream inputStrem = null;
		Properties props = new Properties();
		try {
			inputStrem = new FileInputStream(TestResources.getResourcePath() + "preReg/autoGeneratedId.properties");
			props.load(inputStrem);
			preRegistrationId=props.getProperty("CreatePrereg_All_Valid_Smoke_sid_preRegistrationId");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return preRegistrationId;
	}
	
	public Object[] filterTestCases(Object[] testCases) {
		String testlable = BaseTestCase.testLevel;
		 List<Object> filteredCases = new ArrayList<>();
		 if(testlable.equalsIgnoreCase("smoke")) {
		 for (Object object : testCases)
		 {
			 TestCaseDTO test = (TestCaseDTO) object;
			 if(test.getTestCaseName().toLowerCase().contains(testlable.toLowerCase()))
				 filteredCases.add(object);
		 }
		return filteredCases.toArray();
	}
		 return testCases;
	}
	
	public TestCaseDTO filterOutTestCase(Object[] testCases,String testLabel) {
		 TestCaseDTO test = null;
		 for (Object object : testCases)
		 {
			  test = (TestCaseDTO) object;
			 if(test.getTestCaseName().toLowerCase().contains(testLabel.toLowerCase()))
				 return test;
		 }
		 return test;
	}
	
	public Object[] filterBioTestCases(Object[] testCases, List<String> bioType) {
		String testlable = BaseTestCase.testLevel;
		List<Object> filteredCases = new ArrayList<>();
		if (testlable.equalsIgnoreCase("smoke")) {
			for (Object object : testCases) {
				TestCaseDTO test = (TestCaseDTO) object;
				if (test.getTestCaseName().toLowerCase().contains(testlable.toLowerCase())) {
					for (String bioValue : bioType) {
						String testcase = test.getTestCaseName().toLowerCase();
						if (testcase.contains(bioValue.toLowerCase())) {
							filteredCases.add(object);
						}
					}
				}
			}
		} else {
			if (bioType != null && !bioType.isEmpty()) {
				for (Object object : filteredCases) {
					TestCaseDTO test = (TestCaseDTO) object;
					for (String bioValue : bioType) {
						String testcase = test.getTestCaseName().toLowerCase();
						if (!testcase.equalsIgnoreCase("smoke") && testcase.contains(bioValue.toLowerCase())) {
							filteredCases.add(object);
						}
					}
				}
			}
		}
		return filteredCases.toArray();
	}
	
	protected static String addContextToUrl(String url) {
		if(url.contains("?"))
		{
			String urlArr[]=url.split("\\?");
			return urlArr[0] + "/" + System.getProperty("env.user")+"_context?" + urlArr[1];
		}
		else if(url.contains("mockmv")) return url;
		else
		return url + "/" + System.getProperty("env.user")+"_context";
	}
	public static Response getRequest(String url, String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response getResponse = given().relaxedHTTPSValidation().contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).log().all().when().get(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ getResponse.getBody().asString() + "</pre>");
		return getResponse;
	}
	
	public static Response getRequestWithQueryParam(String url, HashMap<String,String> contextKey,String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response getResponse = given().relaxedHTTPSValidation().queryParams(contextKey)
				.accept("*/*").log().all().when().get(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ getResponse.getBody().asString() + "</pre>");
		return getResponse;
	}
	
	public Response postRequest(String url,String body,String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>"+opsToLog+": </b> <br/>"+body + "</pre>");
		Response apiResponse = RestClient.postRequest(url, body, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"+ apiResponse.getBody().asString() + "</pre>");
		return apiResponse;
	}
	public Response putRequestWithBody(String url,String body,String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>"+opsToLog+": </b> <br/>"+body + "</pre>");
		Response puttResponse = given().relaxedHTTPSValidation().body(body).contentType(MediaType.APPLICATION_JSON)
				.accept("*/*").log().all().when().put(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"+ puttResponse.getBody().asString() + "</pre>");
		return puttResponse;
	}
	
	public Response putRequest(String url,String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response putResponse = given().relaxedHTTPSValidation().contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).log().all().when().put(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ putResponse.getBody().asString() + "</pre>");
		return putResponse;
	}
	
	public Response deleteRequest(String url,String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response deleteResponse = given().relaxedHTTPSValidation().contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).log().all().when().delete(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ deleteResponse.getBody().asString() + "</pre>");
		return deleteResponse;
	}
	
	public Response deleteRequestWithQueryParam(String url,HashMap<String,String> contextKey,String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response deleteResponse = given().relaxedHTTPSValidation().queryParams(contextKey)
				.accept("*/*").log().all().when().delete(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ deleteResponse.getBody().asString() + "</pre>");
		return deleteResponse;
	}
	
	public Response putRequestWithQueryParamAndBody(String url, String body, HashMap<String,String> contextKey, String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response apiResponse = RestClient.putRequestWithQueryParamAndBody(url, body, contextKey,MediaType.APPLICATION_JSON,
				"*/*");
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ apiResponse.getBody().asString() + "</pre>");
		return apiResponse;
	}
	
	public Response postRequestWithQueryParamAndBody(String url, String body, HashMap<String,String> contextKey, String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response apiResponse = RestClient.postRequestWithQueryParamAndBody(url, body, contextKey,MediaType.APPLICATION_JSON,
				MediaType.APPLICATION_JSON);
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ apiResponse.getBody().asString() + "</pre>");
		return apiResponse;
	}
	
	public Response postRequestWithPathParamAndBody(String url, String body, HashMap<String,String> contextKey, String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response apiResponse = given().contentType(ContentType.JSON).pathParams(contextKey).body(body)
				.log().all().when().post(url).then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ apiResponse.getBody().asString() + "</pre>");
		return apiResponse;
	}
	
	public Response postReqestWithCookiesAndBody(String url, String body, String token, String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/>" + body + "</pre>");
		Response posttResponse = given().relaxedHTTPSValidation().body(body).contentType(MediaType.APPLICATION_JSON)
				.accept("*/*").log().all().when().cookie("Authorization", token).post(url).then().log().all().extract()
				.response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ posttResponse.getBody().asString() + "</pre>");
		return posttResponse;
	}
	
	public Response putRequestWithQueryParam(String url, HashMap<String, String> queryParams,String opsToLog) {
		url=addContextToUrl(url);
		Reporter.log("<pre> <b>" + opsToLog + ": </b> <br/></pre>");
		Response puttResponse = given().queryParams(queryParams).relaxedHTTPSValidation().log().all().when().put(url)
				.then().log().all().extract().response();
		Reporter.log("<b><u>Actual Response Content: </u></b>(EndPointUrl: " + url + ") <pre>"
				+ puttResponse.getBody().asString() + "</pre>");
		return puttResponse;
	}
	
	public void constantIntializer() {
		E2EConstants.MACHINE_ID=props.getProperty("machine_id");
		E2EConstants.CENTER_ID=props.getProperty("center_id");
		E2EConstants.USER_ID=props.getProperty("user_id");
		E2EConstants.USER_PASSWD=props.getProperty("user_passwd");
		E2EConstants.SUPERVISOR_ID=props.getProperty("supervisor_id");
		E2EConstants.PRECONFIGURED_OTP=props.getProperty("preconfigured_otp");
	}
	
	
	public static String getBioValueFromJson(String filePath) {
		String bioMetricData = null;
		try {
			String jsonObj = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
			bioMetricData = JsonPrecondtion.getValueFromJson(jsonObj, "response.(documents)[0].value");
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return bioMetricData;
	}
	 
	
}
