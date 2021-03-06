package edu.cnm.deepdive.dominionendpointtestspring.model.dao;


import edu.cnm.deepdive.dominionendpointtestspring.model.entity.GamePlayer;
import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Player;
import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Player.PlayerStateInGame;
import java.util.Optional;
import javax.swing.text.html.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends CrudRepository<Player,Long> {




  Optional<Player> findByOauthKey(String uid);

  Player getByPlayerStateInGame(PlayerStateInGame playerStateInGame);


}