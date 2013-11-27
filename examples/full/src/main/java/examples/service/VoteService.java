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
    Long pos = (Long) sessionFactory.getCurrentSession().createQuery("select count(*) from Vote as vote where " +
        "vote.value = true").uniqueResult();
    Long neg = (Long) sessionFactory.getCurrentSession().createQuery("select count(*) from Vote as vote where " +
        "vote.value = false").uniqueResult();

    return ImmutableMap.of("pos", pos, "neg", neg);
  }

  @Transactional
  public Vote addVote(String name, Boolean value) {
    Vote vote = new Vote(name, value);
    sessionFactory.getCurrentSession().save(vote);
    return vote;
  }
}
