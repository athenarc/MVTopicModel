package org.madgik.persistence.entities;

import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "journal")
@DynamicUpdate
public class Journal extends AbstractEntity {

    @Column(name = "title")
    private String title;

    public Journal() {

    }


    public Journal(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public Journal(String title) {
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
        return "Journal{" +
                "title='" + title + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
