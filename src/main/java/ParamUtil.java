import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.CommonParams;

public class ParamUtil {
	public static void main(String[] args) throws IOException {
		new ParamUtil();
	}
	
	public ParamUtil() throws IOException {
		String payload = IOUtils.toString(getClass().getResourceAsStream("/test_params.txt"), "UTF-8");
		payload = Arrays.asList(payload.split("\n")).stream().reduce("", (r, e) -> r+"&"+e);
		
		CommonParams params = convertToObj(payload, CommonParams.class);
		int a = 1;
	}
	
	private static boolean isInt(String num) {
		try {
			Integer.parseInt(num);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	private static boolean isList(Object obj) {
		if (obj.getClass().getSimpleName().equals(ArrayList.class.getSimpleName())) {
			return true;
		} 
		
		return false;
	}
	
	private static boolean isMap(Object obj) {
		if (obj.getClass().getSimpleName().equals(HashMap.class.getSimpleName())) {
			return true;
		} 
		
		return false;
	}
	
	private static String camelCase(String expr) {
		return expr.substring(0, 1).toLowerCase()+expr.substring(1);
	}
	
	private static <T> T convertToObj(String payload, Class<T> clazz) throws JsonProcessingException {
		Map<String, Object> map = new HashMap<>();
		
		String []keyVals = payload.split("&");
		
		for (String keyVal : keyVals) {
			String []elems = keyVal.split("=");
			if (elems.length != 2) {
				continue;
			}
			
			String key = elems[0].trim();
			String val = elems[1].trim();
			
			String []subKeys = key.split("\\.");
			
			Stack<Object> objects = new Stack<>();
			Stack<Integer> idxes = new Stack<>();
			
			for (int i=0; i<subKeys.length; i++) {
				if (i+1 < subKeys.length) {
					if (i+2 < subKeys.length && 
						subKeys[i+1].equals("member") && isInt(subKeys[i+2])) {
						if (objects.isEmpty()) {
							Object obj = map.get(camelCase(subKeys[i]));
							if (obj == null) {
								obj = new ArrayList();
								map.put(camelCase(subKeys[i]), obj);							
							}
							objects.push(obj);
						} else {
							Object obj = objects.peek();
							if (isMap(obj)) {
								Object o = ((Map)obj).get(camelCase(subKeys[i]));
								if (o == null) {
									o = new ArrayList();
									((Map)obj).put(camelCase(subKeys[i]), o);
								}
								objects.push(o);
							} else if (isList(obj)) {
								try {
									Object o = ((List)obj).get(idxes.peek());
									objects.push(o);
								} catch (IndexOutOfBoundsException e) {
									Object o = new ArrayList();
									((List)obj).add(o);
									objects.push(o);
								}
							}
						}
					} else if (isInt(subKeys[i])) {
						List l = (List)objects.peek();
						try {
							int idx = Integer.parseInt(subKeys[i]);
							idx--;
							idxes.push(idx);
							Object obj = l.get(idx);
							objects.push(obj);
						} catch (IndexOutOfBoundsException e) {
							Map m = new HashMap();
							l.add(m);
							objects.push(m);
						}
					} else if (subKeys[i].equals("member")) {
						continue;
					} else {
						if (objects.isEmpty()) {
							Object obj = map.get(camelCase(subKeys[i]));
							if (obj == null) {
								obj = new HashMap();
								map.put(camelCase(subKeys[i]), obj);
								objects.push(obj);
							} else {
								objects.push(obj);
							}
						} else {
							Object obj = objects.peek();
							if (isMap(obj)) {
								if (i+2 < subKeys.length &&
									subKeys[i+1].equals("member") && isInt(subKeys[i+2])) {
									Object o = new ArrayList();
									((Map)obj).put(camelCase(subKeys[i]), o);
									objects.push(o);
								} else {
									Object o = new HashMap();
									((Map)obj).put(camelCase(subKeys[i]), o);
									objects.push(o);
								}
							} else if (isList(obj)) {
								if (i+2 < subKeys.length &&
										subKeys[i+1].equals("member") && isInt(subKeys[i+2])) {
									Object o = new ArrayList();
									((List)obj).add(o);
									objects.push(o);
								} else {
									Object o = new HashMap();
									((List)obj).add(o);
									objects.push(o);
								}
							}
						}
					}
				} else {
					if (objects.isEmpty() == false) {
						while (objects.isEmpty() == false) {
							Object o = objects.pop();
							if (isMap(o)) {
								((Map)o).put(camelCase(subKeys[i]), val);
								break;
							} 
						}
						objects.clear();
						idxes.clear();
					} else {
						map.put(camelCase(subKeys[i]), val);
					}
				}
			}
		}
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		String json = mapper.writeValueAsString(map);
		
		return mapper.readValue(json, clazz);
	}
}
