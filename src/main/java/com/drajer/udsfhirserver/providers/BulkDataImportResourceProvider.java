package com.drajer.udsfhirserver.providers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.json.JSONArray;
import org.json.JSONObject;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;




public class BulkDataImportResourceProvider {
	
	
	@Operation(name = "$import", idempotent = false)
	public MethodOutcome processMessage(HttpServletRequest theServletRequest, RequestDetails requestDetails,@ResourceParam Parameters parameters){
		System.out.println("Inside import");
		if (requestDetails.getHeader("Prefer") != null && requestDetails.getHeader("Accept") != null) {
			if (requestDetails.getHeader("Prefer").equals("respond-async")
					&& requestDetails.getHeader("Accept").equals("application/fhir+json")) {
				Map response = new HashMap();
				String fileResponse = null;
				String jsonString = null;
				List<ParametersParameterComponent> parameterList = parameters.getParameter();
				for(ParametersParameterComponent parameterObj:parameterList) {
					if(parameterObj.getName().equals("exportType")) {
                    	Type value = parameterObj.getValue();
						if(value instanceof StringType) {
							String exportString = value.toString();
							if(!exportString.equals("static")) {
								throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Please send a valid exportType ");
							}
						}
					}
					if(parameterObj.getName().equals("exportUrl")) {
						Type value = parameterObj.getValue();
						if(value instanceof StringType) {
							try {
								 response = WebClient.builder().baseUrl(value.toString()).build().get()
							     .retrieve().bodyToMono(Map.class).block();
								 JSONObject jsonResponse = new JSONObject(response);
								 JSONArray inputArray =  (JSONArray) jsonResponse.get("input");
								 for(int i=0;i<inputArray.length();i++) {
									 fileResponse = WebClient.builder().baseUrl(inputArray.getJSONObject(i).get("url").toString()).defaultHeader("Accept","application/fhir+json").build().get()
										     .retrieve().bodyToMono(String.class).block();
									 System.err.println(fileResponse);
									 try {
									      FileWriter jsonDataWriter = new FileWriter("src/main/resources/"+inputArray.getJSONObject(i).get("type").toString()+".ndjson");
									      jsonDataWriter.write(fileResponse);
									      jsonDataWriter.close();
									    } catch (IOException e) {
									      System.out.println("An error occurred.");
									      e.printStackTrace();
									    }
								 }
							} catch (Exception e) {
								throw e;
							}
						}
						
					}
					
					
				}
			}
		}
		return new MethodOutcome().setCreated(true);
	}
}

