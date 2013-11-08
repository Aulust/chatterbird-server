package chatterbird.handler.admin;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class UserService {
  @Autowired
    SessionFactory sessionFactory;

  public List<User> getAllUsers() {
    //return this.userRepository.getAllUsers();
    return null;
  }

  public Integer createUser(User user) {
    sessionFactory.getCurrentSession().save(user);
    return null;
  }
}
