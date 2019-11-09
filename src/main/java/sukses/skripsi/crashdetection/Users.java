package sukses.skripsi.crashdetection;

public class Users {
    private String Email;
    private String Kecepaatan;
    private String Latitude;
    private String Longitude;
    private String Name;
    private String NoHp;
    private String Password;
    public Users(){

    }
    public Users(String Email,String Kecepaatan, String Latitude, String Longitude, String Name, String NoHp, String Password){
        this.Email = Email;
        this.Kecepaatan = Kecepaatan;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.Name = Name;
        this.NoHp = NoHp;
        this.Password = Password;
    }
    public Users(String Name, String NoHp){
        this.Name = Name;
        this.NoHp = NoHp;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getKecepaatan() {
        return Kecepaatan;
    }

    public void setKecepaatan(String kecepatan) {
        Kecepaatan = kecepatan;
    }

    public String getLatitude() {
        return Latitude;
    }

    public void setLatitude(String latitude) {
        Latitude = latitude;
    }

    public String getLongitude() {
        return Longitude;
    }

    public void setLongitude(String longitude) {
        Longitude = longitude;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getNoHp() {
        return NoHp;
    }

    public void setNoHp(String hoHp) {
        NoHp = hoHp;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }
}
