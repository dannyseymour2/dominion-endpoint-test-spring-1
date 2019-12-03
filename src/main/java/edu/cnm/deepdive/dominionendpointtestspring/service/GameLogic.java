package edu.cnm.deepdive.dominionendpointtestspring.service;

import edu.cnm.deepdive.dominionendpointtestspring.model.DAO.GameRepository;
import edu.cnm.deepdive.dominionendpointtestspring.model.DAO.PlayRepository;
import edu.cnm.deepdive.dominionendpointtestspring.model.DAO.PlayerRepository;
import edu.cnm.deepdive.dominionendpointtestspring.model.DAO.TurnRepository;
import edu.cnm.deepdive.dominionendpointtestspring.model.aggregates.GameStateInfo;
import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Player;
import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Player.PlayerState;
import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Turn;
import edu.cnm.deepdive.dominionendpointtestspring.model.pojo.Card;
import edu.cnm.deepdive.dominionendpointtestspring.model.entity.Game;
import edu.cnm.deepdive.dominionendpointtestspring.model.pojo.Card.CardType;
import edu.cnm.deepdive.dominionendpointtestspring.state.GameEvents;
import edu.cnm.deepdive.dominionendpointtestspring.state.GameStates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Service
@Transactional
@WithStateMachine
public class GameLogic {

  private Game game;

  private StateMachine<GameStates, GameEvents> stateMachine;


  @Autowired
  GameRepository gameRepository;

  @Autowired
  TurnRepository turnRepository;

  @Autowired
  PlayRepository playRepository;

  @Autowired
  PlayerRepository playerRepository;

  @Autowired
  PlayerService playerService;

  public Turn getCurrentTurn(){
    return turnRepository.getAllByOrderByTurnIdDesc().get(turnRepository.getAllByOrderByTurnIdDesc().size()-1);
  }
  public Game getCurrentGame(){
    return gameRepository.getAllByOrderByIdDesc().get(gameRepository.getAllByOrderByIdDesc().size()-1);
  }


  public GameStateInfo playCardWithCards(CardType cardType, Player player, ArrayList<Card> cards, GameStateInfo gameStateInfo) {

    Card playingCard = new Card(cardType);
    playingCard.getCardType().play(gameStateInfo, Optional.ofNullable(cards));
    if(gameStateInfo.getCurrentPlayerStateInfo().getTurn().getActionsRemaining()==0){
      endActions(gameStateInfo);
      gameStateInfo.getCurrentPlayerStateInfo().setPhaseState(GameStates.PLAYER_1_BUYING);
      signalMachine(GameEvents.PLAYER_1_END_ACTIONS);
    }
    //gameStateInfo.saveAll();
    return gameStateInfo;
  }


  void signalMachine(GameEvents event) {
    Message<GameEvents> message = MessageBuilder
        .withPayload(event)
        .setHeader("Event Transition", event.toString())
        .build();
    stateMachine.sendEvent(message);
  }



  public GameStateInfo buyTarget(CardType cardType, Player player, GameStateInfo gameStateInfo){

    Card buyCard = new Card(cardType);
    int buyingPower = gameStateInfo.getCurrentPlayerStateInfo().calculateBuyingPower()- buyCard.getCost();
    if (buyingPower < 0){
      endTurn(getCurrentGame(), player, gameStateInfo.getOtherPlayerStateInfo().getPlayer());
    }else{
      gameStateInfo.getCurrentPlayerStateInfo().getDiscardPile().addToDiscard(buyCard);
      switch(cardType){
        case PROVINCE:
        case DUCHY:
        case ESTATE:
          testForVictory();
          break;
        default:
          break;
      }
      int buysRemaining = gameStateInfo.getCurrentPlayerStateInfo().calculateBuyingPower()-1;
      if(buysRemaining <=0){
        gameStateInfo.getCurrentPlayerStateInfo().getTurn().setBuysRemaining(buysRemaining);
       // gameStateInfo.saveAll();
        endTurn(getCurrentGame(), gameStateInfo.getCurrentPlayer().get(), gameStateInfo.getOtherPlayerStateInfo().getPlayer());
        return gameStateInfo;
      }else {
        gameStateInfo.getCurrentPlayerStateInfo().getTurn().setBuysRemaining(buysRemaining);

        return gameStateInfo;
      }
    }
    return gameStateInfo;
  }
  public boolean testForVictory(){

    //testForVictory(currentGameState);
    return false;
  }
  public GameStateInfo discard(GameStateInfo gameStateInfo, Card... cards) {
    ArrayList discardCards = new ArrayList(Arrays.asList(cards.clone()));
    gameStateInfo.getCurrentPlayerStateInfo().getDiscardPile().addToDiscard(discardCards);
    //gameStateInfo.saveAll();
    return gameStateInfo;
  }
  public HashMap<String, Integer> initializeStacks() {
      HashMap<String, Integer> stack = new HashMap<>();
      stack.put("Copper", 60);
      stack.put("Silver", 40);
      stack.put("Gold", 30);
      stack.put("Estate", 24);
      stack.put("Duchy", 12);
      stack.put("Province", 12);
      stack.put("Cellar", 10);
      stack.put("Moat", 10);
      stack.put("Village", 10);
      stack.put("Workshop", 10);
      stack.put("Smithy", 10);
      stack.put("Remodel", 10);
      stack.put("Militia", 10);
      stack.put("Market", 10);
      stack.put("Mine", 10);
      stack.put("Merchant", 10);
      stack.put("Trash", 0);
      return stack;
    }


  public void initTurn(Player thisPlayer, Player otherPlayer){
    Turn thisTurn = new Turn(getCurrentGame(), thisPlayer);
    turnRepository.save(thisTurn);
    GameStateInfo gameStateInfo = new GameStateInfo(getCurrentGame(), thisTurn, thisPlayer, otherPlayer);
    if(gameStateInfo.getPreviousTurns().get(thisTurn.getTurnId()-1).isDidAttack()){
      gameStateInfo.getCurrentPlayer().get().setPlayerState(PlayerState.MILITIA_RESPONSE);
    }else {
      gameStateInfo.getCurrentPlayer().get().setPlayerState(PlayerState.ACTION);
    }
    //gameStateInfo.saveAll();
  }
  public void endDiscarding(GameStateInfo gameStateInfo){
    gameStateInfo.getCurrentPlayer().get().setPlayerState(PlayerState.ACTION);
   // gameStateInfo.saveAll();
  }
  public void endActions(GameStateInfo gameStateInfo){
    gameStateInfo.getCurrentPlayer().get().setPlayerState(PlayerState.BUYING);
    gameStateInfo.getCurrentPlayerStateInfo().calculateBuyingPower();
   // gameStateInfo.saveAll();
  }

  public void endTurn(Game game, Player player, Player otherPlayer){
    GameStateInfo gameStateInfo = new GameStateInfo(getCurrentGame(),getCurrentTurn(), player, otherPlayer);
    gameStateInfo.getCurrentPlayer().get().setPlayerState(PlayerState.WATCHING);
    if (gameStateInfo.getCurrentPlayer().get().getId() == 1) {
      signalMachine(GameEvents.PLAYER_1_END_BUYS);
    }else{
      signalMachine(GameEvents.PLAYER_2_END_BUYS);
    }
    //gameStateInfo.saveAll();
    //TODO update other player
  }
  /**
  public List updateOtherPlayer(){
    ArrayList<Turn> allTurns = turnRepository.getAllByOrderByTurnIdDesc();
    Turn lastTurn = allTurns.get(allTurns.size()-2);
    return (List) playRepository.getAllByTurn(lastTurn);
  }
*/
  public void endGame(){
    signalMachine(GameEvents.END_GAME);
  }


  public GameStateInfo militiaDiscard(Card card, Player player, GameStateInfo gameStateInfo) {

    return gameStateInfo;
  }
}
