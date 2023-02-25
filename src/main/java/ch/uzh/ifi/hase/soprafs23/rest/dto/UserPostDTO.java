package ch.uzh.ifi.hase.soprafs23.rest.dto;

import java.util.Date;

public class UserPostDTO {

  private String name;

  private String username;

  private String password;

    private Date creation_date;
    private Date birthday;

    public Date getBirthday(){
        return birthday;
    }
    public void setBirthday(Date birthday){
        this.birthday = birthday;
    }
    public Date getCreation_date() {
        return creation_date;}
    public void setCreation_date(Date creation_date) {
        this.creation_date = creation_date;
    }
    public String getPassword() {
         return password;
    }
    public void setPassword(String password){
            this.password = password;
     }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
