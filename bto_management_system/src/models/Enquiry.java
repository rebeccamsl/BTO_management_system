package models; // Updated package

import enums.EnquiryStatus; // Updated import
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Enquiry implements Serializable {
    private static final long serialVersionUID = 1L;
    private static AtomicInteger idCounter = new AtomicInteger(0);

    private int enquiryId;
    private String submitterNric;
    private int projectId;
    private String content;
    private List<String> replies;
    private EnquiryStatus status;
    private Date submissionDate;
    private Date lastUpdateDate;

    // Constructor for new enquiry
    public Enquiry(String submitterNric, int projectId, String content) {
        this.enquiryId = idCounter.incrementAndGet();
        this.submitterNric = submitterNric;
        this.projectId = projectId;
        this.content = content;
        this.replies = new ArrayList<>();
        this.status = EnquiryStatus.OPEN;
        this.submissionDate = new Date();
        this.lastUpdateDate = this.submissionDate;
    }

      // Constructor for loading existing enquiry
    public Enquiry(int enquiryId, String submitterNric, int projectId, String content,
                   List<String> replies, EnquiryStatus status, Date submissionDate, Date lastUpdateDate) {
        this.enquiryId = enquiryId;
        this.submitterNric = submitterNric;
        this.projectId = projectId;
        this.content = content;
        this.replies = replies != null ? new ArrayList<>(replies) : new ArrayList<>();
        this.status = status;
        this.submissionDate = submissionDate;
        this.lastUpdateDate = lastUpdateDate;
         if (enquiryId >= idCounter.get()) {
             idCounter.set(enquiryId + 1);
         }
    }

    // Getters
    public int getEnquiryId() { return enquiryId; }
    public String getSubmitterNric() { return submitterNric; }
    public int getProjectId() { return projectId; }
    public String getContent() { return content; }
    public List<String> getReplies() { return new ArrayList<>(replies); }
    public EnquiryStatus getStatus() { return status; }
    public Date getSubmissionDate() { return submissionDate; }
    public Date getLastUpdateDate() { return lastUpdateDate; }

    // Setters / Modifiers
    public void setContent(String content) {
        if (this.status != EnquiryStatus.CLOSED) {
            this.content = content;
            this.lastUpdateDate = new Date();
        } else {
            System.err.println("Cannot edit a closed enquiry.");
        }
    }
    public void addReply(String replierInfo, String replyText) {
        String formattedReply = String.format("[%s] %s: %s", new Date(), replierInfo, replyText);
        this.replies.add(formattedReply);
        this.status = EnquiryStatus.ANSWERED;
        this.lastUpdateDate = new Date();
    }
    public void closeEnquiry() {
        this.status = EnquiryStatus.CLOSED;
        this.lastUpdateDate = new Date();
    }
    public void setStatus(EnquiryStatus status) {
        this.status = status;
        this.lastUpdateDate = new Date();
    }

    @Override
    public String toString() {
        return "Enquiry{" +
               "enquiryId=" + enquiryId + ", submitterNric='" + submitterNric + '\'' + ", projectId=" + projectId +
               ", status=" + status + ", content='" + content + '\'' + '}';
    }
    public static void resetIdCounter() { idCounter.set(0); }
    public static void updateIdCounter(int maxId) { if (maxId >= idCounter.get()) idCounter.set(maxId + 1); }
}