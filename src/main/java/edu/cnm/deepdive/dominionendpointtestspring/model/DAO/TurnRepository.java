package edu.cnm.deepdive.dominionendpointtestspring.model.dao;

import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Turn;

import java.util.ArrayList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnRepository extends JpaRepository<Turn,Long> {



  //Turn getTurnById(int id);
  

  ArrayList<Turn> getAllByOrderByTurnIdDesc();





  //boolean existsById(int turnId);



  long count();


  void delete(Turn turn);

  void deleteAll(Iterable<? extends Turn> iterable);
}