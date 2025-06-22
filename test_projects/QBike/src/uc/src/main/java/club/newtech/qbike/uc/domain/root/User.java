package club.newtech.qbike.uc.domain.root;

import club.newtech.qbike.uc.domain.Type;
import lombok.Data;

import javax.persistence.*;

import static javax.persistence.EnumType.STRING;

@Entity
@Table(name = "T_USER")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(length = 64)
    private String userName;
    @Column(length = 64, nullable = false)
    private String mobile;
    @Column(length = 64)
    private String province;
    @Column(length = 64)
    private String city;
    @Column(length = 64)
    private String district;
    private String street;
    private String originAddress;


    @Enumerated(value = STRING)
    @Column(length = 32, nullable = false)
    private Type type;

}
