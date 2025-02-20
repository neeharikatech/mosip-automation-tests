package org.mosip.dataprovider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.mosip.dataprovider.mds.MDSClient;
import org.mosip.dataprovider.models.ApplicationConfigIdSchema;
import org.mosip.dataprovider.models.BiometricDataModel;
import org.mosip.dataprovider.models.Contact;
import org.mosip.dataprovider.models.DynamicFieldModel;
import org.mosip.dataprovider.models.DynamicFieldValueModel;
import org.mosip.dataprovider.models.IrisDataModel;
import org.mosip.dataprovider.models.MosipDocument;
import org.mosip.dataprovider.models.MosipGenderModel;
import org.mosip.dataprovider.models.MosipIndividualTypeModel;
import org.mosip.dataprovider.models.MosipLanguage;


import org.mosip.dataprovider.models.MosipPreRegLoginConfig;
import org.mosip.dataprovider.models.Name;
import org.mosip.dataprovider.models.ResidentModel;
import org.mosip.dataprovider.preparation.MosipMasterData;
import org.mosip.dataprovider.util.CommonUtil;
import org.mosip.dataprovider.util.DataProviderConstants;
import org.mosip.dataprovider.util.Gender;
import org.mosip.dataprovider.util.ResidentAttribute;
import org.mosip.dataprovider.util.RestClient;
import org.mosip.dataprovider.util.Translator;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;

import variables.VariableManager;

/*
 * Generate Resident record
 * First Name MiddleName SurName ...
 * 
 * By default all attributes are selected.
 * If not set 'Any' is used
 *  Can set to 'No' to skip the attribute in output
 *  For Finger ->
 *     'All' -> all ten fingers
 *     String[] -> names of fingers - LeftThumb, RightThumb etc
 *  
 */
public class ResidentDataProvider {
		Properties attributeList;
	
	
	public ResidentDataProvider() {
		attributeList = new Properties();
		attributeList.put(ResidentAttribute.RA_Count, 1);
		//attributeList.put(ResidentAttribute.RA_PRIMARAY_LANG, DataProviderConstants.LANG_CODE_ENGLISH);
		//attributeList.put(ResidentAttribute.RA_Country, "PHIL");
		RestClient.clearToken();
	}
	/*
	public static ResidentModel readPersona(String filePath) throws IOException {
    	
    	ObjectMapper mapper = new ObjectMapper();
    	Path path = Paths.get(filePath);
    	//mapper.registerModule(new SimpleModule().addDeserializer(Pair.class,new PairDeserializer()));
    //	mapper.registerModule(new SimpleModule().addSerializer(Pair.class, new PairSerializer()));
    	byte[] bytes = Files.readAllBytes(path);
		return mapper.readValue(bytes, ResidentModel.class);
    }
	*/
	//Attribute Value ->'Any','No' or specific value
	public ResidentDataProvider addCondition(ResidentAttribute attributeName, Object attributeValue) {
		attributeList.put(attributeName, attributeValue);
		return this;
	}
	public static ResidentModel genGuardian(Properties attributes) {
		Properties attributeList = new Properties();
		attributes.forEach( (k,v) ->{
			attributeList.put(k, v);
		});
		attributeList.put(ResidentAttribute.RA_Count, 1);
		attributeList.put(ResidentAttribute.RA_Age, ResidentAttribute.RA_Adult);
		attributeList.put(ResidentAttribute.RA_Gender, Gender.Any);
		
		ResidentDataProvider provider = new ResidentDataProvider();
		provider.attributeList = attributeList;
		ResidentModel guardian = provider.generate().get(0);
		return guardian;
	}
	public static ResidentModel updateBiometric(ResidentModel model,String bioType) throws Exception {
		boolean bDirty = false;
		
		if(bioType.equalsIgnoreCase("finger")) {
			BiometricDataModel bioData = BiometricDataProvider.getBiometricData(true);
			model.getBiometric().setFingerPrint( bioData.getFingerPrint());
			model.getBiometric().setFingerHash( bioData.getFingerHash());
			bDirty = true;
		}
		else
		if(bioType.equalsIgnoreCase("iris")) {
			List<IrisDataModel> iris = BiometricDataProvider.generateIris(1);
			if(iris != null && !iris.isEmpty()) {
				model.getBiometric().setIris(iris.get(0));
				bDirty = true;
			}
		}
		else
		if(bioType.equalsIgnoreCase("face")) {
			BiometricDataModel bioData = model.getBiometric();
			byte[][] faceData = PhotoProvider.getPhoto(CommonUtil.generateRandomNumbers(1, DataProviderConstants.MAX_PHOTOS, 1)[0], model.getGender().name() );
			bioData.setEncodedPhoto(
					Base64.getEncoder().encodeToString(faceData[0]));
			bioData.setRawFaceData(faceData[1]);
		
			bioData.setFaceHash(CommonUtil.getHexEncodedHash( faceData[1]));
			bDirty = true;
		}
		if(bDirty)
			model.getBiometric().setCbeff(null); 
			 
		return model;
	}
	private static String[] getConfiguredLanguages() {
		String [] lang_arr = null;
		List<String> langs= new ArrayList<String>();
		List<MosipLanguage> allLang = null;
		try {
			allLang = MosipMasterData.getConfiguredLanguages();
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		MosipPreRegLoginConfig  preregconfig = MosipMasterData.getPreregLoginConfig();
		if(preregconfig == null) {

			try {
			
				lang_arr = new String[allLang.size()];
				int i=0;
				for(MosipLanguage l: allLang){
					lang_arr[i]= l.getIso2();
					i++;
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
			return lang_arr;
		}
		//check if primary lang is already configured
		String primary_lang = preregconfig.getMosip_primary_language();
		if(primary_lang != null)
			langs.add(primary_lang);
		
		//Step1 : check any mandatory languages configured
		String mandatory_languages_list =preregconfig.getMandatory_languages();
		 String[] mandatlangueages = null;
		if(mandatory_languages_list !=null && !mandatory_languages_list.equals("")) {
			  mandatlangueages = mandatory_languages_list.split(",");
		}
		int minLanguages=Integer.parseInt(preregconfig.getMin_languages_count());
		String opt_lang_list = preregconfig.getOptional_languages();
		String[] opt_langs = null;
		if(opt_lang_list != null && !opt_lang_list.equals("")) {
			opt_langs = opt_lang_list.split(",");
		}
		if(mandatlangueages != null && mandatlangueages.length > 0 ) {
			for(int i=0; i < mandatlangueages.length; i++)
				langs.add( mandatlangueages[i]);
		}
		if(opt_langs != null && opt_langs.length >0) {
			for(int i=0; i < opt_langs.length; i++)
				langs.add( opt_langs[i]);
		}
		//if not enough languags add from the default master datalist
		if(minLanguages > 0  && langs.size() < minLanguages && allLang != null ) {
			int n2add = minLanguages - langs.size();
			for(int i= 0; i < allLang.size() && i < n2add; i++ ) {
				langs.add( allLang.get(i).getIso2());
			}
		}
		lang_arr = new String [ minLanguages > 0 ? minLanguages : langs.size()];
		return langs.toArray(lang_arr);
	}
	public List<ResidentModel> generate() {
		
		List<ResidentModel> residents = new ArrayList<ResidentModel>();
		
		int count = (int) attributeList.get(ResidentAttribute.RA_Count);
		Gender gender =  (Gender) attributeList.get(ResidentAttribute.RA_Gender);
		String primary_lang = (String) attributeList.get(ResidentAttribute.RA_PRIMARAY_LANG);
		String sec_lang = (String) attributeList.get(ResidentAttribute.RA_SECONDARY_LANG);
		String override_primary_lan = primary_lang;
		String override_sec_lang = sec_lang;
		String third_lang = (String) attributeList.get(ResidentAttribute.RA_THIRD_LANG);
		
		Object oAttr = attributeList.get(ResidentAttribute.RA_SCHEMA_VERSION);
		double schemaVersion = (oAttr == null) ? 0: (double)oAttr;
		VariableManager.setVariableValue("schemaVersion", schemaVersion);


		String[] langsRequired = getConfiguredLanguages();
		if(langsRequired != null) {
			primary_lang = langsRequired[0];
			if(langsRequired.length > 1)
				sec_lang = langsRequired[1];
			if(langsRequired.length > 2)
				third_lang = langsRequired[2];
			
		}
		/*
		MosipPreRegLoginConfig  preregconfig = MosipMasterData.getPreregLoginConfig();
		if(preregconfig != null) {
			primary_lang = preregconfig.getMosip_primary_language();
		}
		
		String mandatory_languages_list =preregconfig.getMandatory_languages();
		 String[] mandatlangueages = null;
		if(mandatory_languages_list !=null && !mandatory_languages_list.equals("")) {
			  mandatlangueages = mandatory_languages_list.split(",");
		}
		
		List<MosipLanguage> allLang = MosipMasterData.getConfiguredLanguages();
		
		
		  if(primary_lang == null) {
			  String mandatory_languages=preregconfig.getMandatory_languages();
			  if(mandatory_languages!=null && !mandatory_languages.equals("")) {
				  String[] mandatlangueage = mandatory_languages.split(",");
				  primary_lang=mandatlangueage[0].trim();
				  if(mandatlangueage.length>1) {
					  sec_lang= mandatlangueage[1].trim();
				  }
				  if(mandatlangueage.length>2) {
					  third_lang=  mandatlangueage[2].trim();
				  }
			  }
			  
			  if(primary_lang==null) {
				 String languages= preregconfig.getOptional_languages();
				 if(languages!=null && !languages.equals("")) {
					  String[] langueage = languages.split(",");
					  primary_lang=langueage[0].trim();
					  if(langueage.length>1) {
						  sec_lang= langueage[1].trim();
					  }
					  if(langueage.length>2) {
						  third_lang=  langueage[2].trim();
					  }
				  }

			  }
			  
		  }
			 // primary_lang = "eng";
		 
		int minLanguages=Integer.parseInt(preregconfig.getMin_languages_count());
		
		//boolean bFoundSecLang = false;
		for(MosipLanguage lang: allLang) {
			if(!lang.getIsActive())
				continue;
			
			if (primary_lang == null) {
				primary_lang = lang.getCode();
				// bFoundSecLang = true;
				break;
			}
			
			if(sec_lang == null && minLanguages>1) {
				if(!lang.getCode().equals(primary_lang)){
					sec_lang = lang.getCode();
					//bFoundSecLang = true;
					break;
				}
			}
			if(third_lang == null && minLanguages>2) {
				if(!lang.getCode().equals(sec_lang) && !lang.getCode().equals(primary_lang) ){
					third_lang = lang.getCode();
					//bFoundSecLang = true;
					break;
				}
			}
		
		}
	*/
		/*
		 * if(!bFoundSecLang) sec_lang = null;
		 */
		
		//override if specified
		if(override_primary_lan != null && !override_primary_lan.equals(""))
			primary_lang = override_primary_lan;
		
		if(override_sec_lang != null && !override_sec_lang.equals(""))
			sec_lang = override_sec_lang;
		
		oAttr = attributeList.get(ResidentAttribute.RA_Iris);
		boolean bIrisRequired = true;
		
		if(oAttr != null) {
			bIrisRequired = (boolean)oAttr;
		}
		if(gender == null)
			gender  = Gender.Any;
		List<Name> names_sec = null;
		List<Name> names_primary =null;
		
		Hashtable<String,List<DynamicFieldModel>> dynaFields = MosipMasterData.getAllDynamicFields();
		 
		List<MosipGenderModel> genderTypes_primary = MosipMasterData.getGenderTypes(primary_lang);
		List<MosipGenderModel> genderTypes_sec = null;
		List<MosipGenderModel> genderTypes_third = null;
		
		if(sec_lang != null)
			genderTypes_sec = MosipMasterData.getGenderTypes(sec_lang);

		if(third_lang != null)
			genderTypes_third = MosipMasterData.getGenderTypes(third_lang);

		//generate mix of both genders
		int maleCount =0,femaleCount = 0;
		
		switch(gender) {
			case  Any:
				maleCount = count/2;
				femaleCount = count-maleCount;
				break;
			case Male:
				maleCount = count;
				break;
			case Female:
				femaleCount = count;
				break;
			default:
				break;
				
		}
		List<Name> eng_male_names = null;
		List<Name> eng_female_names = null;
		List<Name> eng_names = null;
		
		if(maleCount >0) {
			eng_male_names = NameProvider.generateNames(Gender.Male,  DataProviderConstants.LANG_CODE_ENGLISH, maleCount, null);
			eng_names = eng_male_names;
		}
		if(femaleCount > 0) {
			eng_female_names = NameProvider.generateNames(Gender.Female,  DataProviderConstants.LANG_CODE_ENGLISH, femaleCount, null);
			if(eng_names != null)
				eng_names.addAll(eng_female_names);
			else
				eng_names = eng_female_names;
		}
		
		if(primary_lang != null) {
			if(!primary_lang.startsWith( DataProviderConstants.LANG_CODE_ENGLISH)) {
				names_primary = NameProvider.generateNames(gender, primary_lang, count, eng_names);
			}
			else
				names_primary = eng_names;

		}
		if(sec_lang != null) {
			if(!sec_lang.startsWith( DataProviderConstants.LANG_CODE_ENGLISH)) {
				names_sec = NameProvider.generateNames(gender, sec_lang, count, eng_names);
			}
			else
				names_sec = eng_names;

		}

		List<Contact> contacts = ContactProvider.generate(eng_names, count);
//		Object  objCountry = attributeList.get(ResidentAttribute.RA_Country)  ;
		//String country  =null;
		
	//	if(objCountry != null)
	//		country = objCountry.toString();
		
		//List<Location> locations = LocationProvider.generate(DataProviderConstants.COUNTRY_CODE, count);
		//Hashtable<String, List<MosipLocationModel>> locations =  LocationProvider.generate( count, country);
		
		ApplicationConfigIdSchema locations = LocationProvider.generate(primary_lang, count);
		ApplicationConfigIdSchema locations_secLang  = null;
		if(sec_lang != null)
			locations_secLang = LocationProvider.generate(sec_lang, count);
		
		Hashtable<String,List<DynamicFieldValueModel>> bloodGroups = null;
		if(dynaFields != null && !dynaFields.isEmpty())
			 bloodGroups = BloodGroupProvider.generate(count, dynaFields);

		Hashtable<String, List<MosipIndividualTypeModel>> resStatusList =  MosipMasterData.getIndividualTypes();
		
		int [] idxes = CommonUtil.generateRandomNumbers(count,DataProviderConstants.MAX_PHOTOS,0);

		List<IrisDataModel> irisList = null;
		try {
			if(bIrisRequired)
				irisList = BiometricDataProvider.generateIris(count);
		} catch (  Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Random rand = new Random();
		for(int i=0; i < count; i++) {
			Gender res_gender = names_primary.get(i).getGender();
			ResidentModel res= new ResidentModel();
			res.setPrimaryLanguage(primary_lang);
			res.setSecondaryLanguage(sec_lang);
			res.setDynaFields(dynaFields);
			res.setName(names_primary.get(i));
			res.setThirdLanguage(third_lang);
			
			res.getGenderTypes().put(primary_lang, genderTypes_primary);
			if(sec_lang != null)
				res.getGenderTypes().put(sec_lang, genderTypes_sec);
			if(third_lang != null)
				res.getGenderTypes().put(third_lang, genderTypes_third);
			
			if(attributeList.containsKey(ResidentAttribute.RA_MissList)) {
				res.setMissAttributes( (List<String>) attributeList.get(ResidentAttribute.RA_MissList));
			}
			if(attributeList.containsKey(ResidentAttribute.RA_InvalidList)) {
				res.setInvalidAttributes( (List<String>) attributeList.get(ResidentAttribute.RA_InvalidList));
			}
			res.setGender(res_gender);
			/*
			if(primary_lang.startsWith( DataProviderConstants.LANG_CODE_ENGLISH))
				res.setGender(names_primary.get(i).getGender().name());
			else
				res.setGender(Translator.translate(primary_lang, names_primary.get(i).getGender().name()));
			if(sec_lang != null) {
				if(sec_lang.startsWith( DataProviderConstants.LANG_CODE_ENGLISH))
					res.setGender_seclang(names_primary.get(i).getGender().name());
				else
					res.setGender_seclang(Translator.translate(sec_lang, names_primary.get(i).getGender().name()));
			}*/
			if(names_sec != null) {
				res.setName_seclang(names_sec.get(i));
			}
		
			if(bloodGroups != null && !bloodGroups.isEmpty())
				res.setBloodgroup(bloodGroups.get(res.getPrimaryLanguage()).get(i));
			res.setContact(contacts.get(i));
			res.setDob( DateOfBirthProvider.generate((ResidentAttribute) attributeList.get(ResidentAttribute.RA_Age)));
			ResidentAttribute age =  (ResidentAttribute) attributeList.get(ResidentAttribute.RA_Age);
			Boolean skipGaurdian = false;
			if(age == ResidentAttribute.RA_Minor) {
				res.setMinor(true);
				if(attributeList.containsKey(ResidentAttribute.RA_SKipGaurdian))
					skipGaurdian =   Boolean.valueOf(attributeList.get(ResidentAttribute.RA_SKipGaurdian).toString());
				if(!skipGaurdian)
					res.setGuardian( genGuardian(attributeList));
			}
			res.setAppConfigIdSchema( locations);
			res.setAppConfigIdSchema_secLang(locations_secLang);
			
			res.setLocation(  locations.getTblLocations().get(i));
			String [] addr = new String[ DataProviderConstants.MAX_ADDRESS_LINES];
			String addrFmt = "#%d, %d Street, %d block, lane #%d" ;//+ schemaItem.getId();
			for(int ii=0; ii< DataProviderConstants.MAX_ADDRESS_LINES; ii++) {
				String addrLine = String.format(addrFmt, (10+ rand.nextInt(999)),
					(1 + rand.nextInt(99)),
					(1 + rand.nextInt(10)), ii+1
					);
				addr[ii] = addrLine;
			}

			String primLang = res.getPrimaryLanguage();
			if(!primLang.toLowerCase().startsWith("en"))
			{
				
				String [] addrP = new String[ DataProviderConstants.MAX_ADDRESS_LINES];

				for(int ii=0; ii< DataProviderConstants.MAX_ADDRESS_LINES; ii++) {
					
					addrP[ii] = Translator.translate(primLang, addr[ii]);
				}
				res.setAddress(addrP);
			}
			else
				res.setAddress(addr);
			//res.setLocation(locations.get(res.getPrimaryLanguage()));
			if(res.getSecondaryLanguage() != null) {
				res.setLocation_seclang (  locations_secLang.getTblLocations().get(i));
				String[] addr_sec = new String[DataProviderConstants.MAX_ADDRESS_LINES];
				for(int ii=0; ii< DataProviderConstants.MAX_ADDRESS_LINES; ii++) {
					addr_sec[ii] = Translator.translate(res.getSecondaryLanguage(), addr[ii]);
				}	
				res.setAddress_seclang(addr_sec);
			}
			//	res.setLocation_seclang(locations.get(res.getPrimaryLanguage()));
			
			List<MosipIndividualTypeModel> lstResStatusPrimLang = resStatusList.get( res.getPrimaryLanguage());
			int indx =0;
			if(lstResStatusPrimLang != null) {
				for(MosipIndividualTypeModel itm: lstResStatusPrimLang) {
					if(itm.getCode().equals("NFR")) {
						res.setResidentStatus(itm);
						break;
					}
					indx++;
				}
				if(res.getResidentStatus() == null) {
					indx = rand.nextInt(lstResStatusPrimLang.size());
					res.setResidentStatus(lstResStatusPrimLang.get(indx));
				}
			}
			if(res.getSecondaryLanguage() != null) {
				List<MosipIndividualTypeModel> lstResStatusSecLang = resStatusList.get( res.getSecondaryLanguage());
				if(lstResStatusSecLang != null) {
					for(MosipIndividualTypeModel itm: lstResStatusSecLang) {
						if(itm.getCode().equals(lstResStatusPrimLang.get(indx).getCode())){
							res.setResidentStatus_seclang(itm);
							break;
						}
					}
				}	
				if(res.getResidentStatus_seclang() == null) {
					res.setResidentStatus_seclang(res.getResidentStatus());
				}
			}
			Object bFinger = attributeList.get(ResidentAttribute.RA_Finger);
			Boolean skip =  (bFinger == null ? false: !(Boolean)bFinger);
			res.setSkipFinger(skip);
			bFinger = attributeList.get(ResidentAttribute.RA_Photo);
			skip =  (bFinger == null ? false: !(Boolean)bFinger);
			res.setSkipFace(skip);
			bFinger = attributeList.get(ResidentAttribute.RA_Iris);
			skip =  (bFinger == null ? false: !(Boolean)bFinger);
			res.setSkipIris(skip);
			
			
			BiometricDataModel bioData =null;
			try {
				bioData = BiometricDataProvider.getBiometricData(bFinger == null ? true: (Boolean)bFinger);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			if(bIrisRequired)
				bioData.setIris(irisList.get(i));
			
			Object bOFace = attributeList.get(ResidentAttribute.RA_Photo);
			boolean bFace = ( bOFace == null ? true: (boolean)bOFace);
			if(bFace) {
				byte[][] faceData = PhotoProvider.getPhoto(idxes[i], res_gender.name() );
				bioData.setEncodedPhoto(
						Base64.getEncoder().encodeToString(faceData[0]));
				bioData.setRawFaceData(faceData[1]);
			
				try {
					bioData.setFaceHash(CommonUtil.getHexEncodedHash( faceData[1]));
				} catch (Exception e1) {
				// TODO Auto-generated catch block
				//e1.printStackTrace();
				}
			}
//			res.setEncodedPhoto( );

			res.setBiometric(bioData);
		
			oAttr = attributeList.get(ResidentAttribute.RA_Document);
			boolean bDocRequired = ( oAttr == null ? true: (boolean)oAttr);
			
			if(bDocRequired) {
				try {
					res.setDocuments(DocumentProvider.generateDocuments(res));
				} catch (DocumentException | IOException  | ParseException e) {
					
					e.printStackTrace();
				}
			}
			
			for(MosipDocument doc: res.getDocuments()) {
				String id = doc.getDocCategoryCode();
				int index = CommonUtil.generateRandomNumbers(1, doc.getDocs().size()-1, 0)[0];
				res.getDocIndexes().put(id,index);
			}
			residents.add(res);
		}
		return residents;
	}

	public static void main(String[] args) throws Exception {
		
		ResidentDataProvider residentProvider = new ResidentDataProvider();
		residentProvider.addCondition(ResidentAttribute.RA_Count, 1)
		.addCondition(ResidentAttribute.RA_SECONDARY_LANG, "ara")
		.addCondition(ResidentAttribute.RA_Gender, Gender.Any)
		.addCondition(ResidentAttribute.RA_Age, ResidentAttribute.RA_Adult);
		
		List<ResidentModel> lst =  residentProvider.generate();
		MDSClient cli = new MDSClient(0);
		
		for(ResidentModel r: lst) {
			System.out.println(r.toJSONString());
	
			cli.createProfile("C:\\Mosip.io\\gitrepos\\mosip-mock-services\\MockMDS\\target\\Profile\\", "tst1", r);
			
		}
		
	}
}
