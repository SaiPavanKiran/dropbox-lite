package org.rspk.dropbox_lite.lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.Tagging;

import java.util.List;
import java.util.Map;

public class AutoAddTags implements RequestHandler<Map<String, Object>, Object> {


    @Override
    public Object handleRequest(Map<String, Object> stringObjectMap, Context context) {
        try {
            Map<String, Object> detail = getMap(stringObjectMap, "detail");
            if (detail == null) return null;

            String eventName = (String) detail.get("eventName");
            Map<String, Object> userIdentity = getMap(detail, "userIdentity");
            String arn = userIdentity != null ? (String) userIdentity.get("arn") : null;
            if (arn == null) return null;
            String username = arn.contains("/") ? arn.split("/")[1] : arn;

            Map<String, Object> responseElements = getMap(detail, "responseElements");
            Map<String, Object> requestParameters = getMap(detail, "requestParameters");

            switch (eventName) {
                case "RunInstances":
                    tagEc2Instance(responseElements, username);
                    break;
                case "CreateBucket":
                    tagS3Bucket(requestParameters, responseElements, username);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            context.getLogger().log("Error processing tag creation: " + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> stringObjectMap, String key) {
        if (stringObjectMap != null && stringObjectMap.get(key) instanceof Map) {
            return (Map<String, Object>) stringObjectMap.get(key);
        } else return null;
    }

    @SuppressWarnings("unchecked")
    private void tagEc2Instance(Map<String, Object> responseElements, String username) {
        Map<String, Object> instancesSet = getMap(responseElements, "instancesSet");
        if (instancesSet == null) return;
        List<Map<String, Object>> items = (List<Map<String, Object>>) instancesSet.get("items");
        if (items != null && !items.isEmpty()) {
            String instanceId = (String) items.getFirst().get("instanceId");
            if (instanceId == null) return;
            try (Ec2Client client = Ec2Client.builder().build()) {
                client.createTags(CreateTagsRequest.builder()
                        .resources(instanceId)
                        .tags(Tag.builder().key("Owner").value(username).build())
                        .build());
            }
        }
    }

    private void tagS3Bucket(Map<String, Object> requestParameters, Map<String, Object> responseElements, String username) {
        String bucketName = null;
        if (requestParameters != null) {
            bucketName = (String) requestParameters.get("bucketName");
        }
        if (bucketName == null && responseElements != null) {
            bucketName = (String) responseElements.get("bucketName");
        }

        if (bucketName != null) {
            try (S3Client s3Client = S3Client.builder().build()) {
                software.amazon.awssdk.services.s3.model.Tag tag = software.amazon.awssdk.services.s3.model.Tag.builder()
                        .key("Owner")
                        .value(username)
                        .build();
                Tagging tagging = Tagging.builder().tagSet(tag).build();
                s3Client.putBucketTagging(PutBucketTaggingRequest.builder()
                        .bucket(bucketName)
                        .tagging(tagging)
                        .build());
            }
        }
    }
}
