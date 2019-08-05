package org.madgik.dtos;

public class JournalDto extends AbstractDto {

    private String title;

    public JournalDto() {

    }

    public JournalDto(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public JournalDto(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "JournalDto{" +
                "title='" + title + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
