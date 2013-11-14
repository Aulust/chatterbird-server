package chatterbird.handler.admin;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public class UserRepository {
  @Autowired
  SessionFactory sessionFactory;

  public List<User> getAllUsers() {
    //sessionFactory.getCurrentSession().load()
    //return this.hibernateTemplate.loadAll(User.class);
    return null;
  }

  @Transactional
  public Integer createUser(User user) {
    //Transaction trans=sessionFactory.openSession().beginTransaction();
    sessionFactory.getCurrentSession().save(user);
    //sessionFactory.getCurrentSession().flush();
    //trans.commit();
    return null;
  }
}
