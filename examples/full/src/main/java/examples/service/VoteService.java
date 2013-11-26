package examples.service;

import com.google.common.collect.ImmutableMap;
import examples.model.Vote;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class VoteService {
  @Autowired
  SessionFactory sessionFactory;

  @Transactional
  @SuppressWarnings("unchecked")
  public Map<String, Long> countVotes() {
    Map<String, Long> counts = (Map<String, Long>) sessionFactory.getCurrentSession().createQuery("select new Map(" +
        "(select count(*) from Vote as vote where vote.value = true) as pos," +
        "(select count(*) from Vote as vote where vote.value = false) as neg) " +
        "from Vote").uniqueResult();

    if (counts == null) {
      counts = ImmutableMap.of("pos", (long) 0, "neg", (long) 0);
    }

    return counts;
  }

  @Transactional
  public Vote addVote(String name, Boolean value) {
    Vote vote = new Vote(name, value);
    sessionFactory.getCurrentSession().save(vote);
    return vote;
  }
}
