package org.madgik.dtos;

public class DocJournalDto extends AbstractDto{


    public String getDocid() {
        return docid;
    }

    public void setDocid(String docid) {
        this.docid = docid;
    }

    public String getJournalid() {
        return journalid;
    }

    public void setJournalid(String journalid) {
        this.journalid = journalid;
    }

    public DocJournalDto(String docid, String journalid) {
        this.docid = docid;
        this.journalid = journalid;
    }

    private String docid;
    private String journalid;

}
