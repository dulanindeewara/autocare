package lk.sampath_autocare.asset.role.entity;


import lk.sampath_autocare.asset.user.entity.User;
import lk.sampath_autocare.util.audit.AuditEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Role extends AuditEntity {

    @NotNull
    @Column( unique = true )
    private String roleName;

    @ManyToMany(mappedBy = "roles")
    private List< User > users;
}
