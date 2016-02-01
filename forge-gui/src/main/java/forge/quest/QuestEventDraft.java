/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.quest;

import forge.GuiBase;
import forge.card.CardEdition;
import forge.card.CardEdition.CardInSet;
import forge.card.CardRarity;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.limited.BoosterDraft;
import forge.limited.LimitedPoolType;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.ReadPriceList;
import forge.util.NameGenerator;
import forge.util.storage.IStorage;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>
 * QuestEvent.
 * </p>
 *
 * MODEL - A basic event instance in Quest mode. Can be extended for use in
 * unique event types: battles, quests, and others.
 */
public class QuestEventDraft {

    public static class QuestDraftPrizes {

        public int credits;
        public List<BoosterPack> boosterPacks;
        public List<PaperCard> individualCards;
        public List<PaperCard> selectRareCards;

        public boolean hasCredits() {
            return credits > 0;
        }

        public boolean hasBoosterPacks() {
            return boosterPacks != null && !boosterPacks.isEmpty();
        }

        public boolean hasIndividualCards() {
            return individualCards != null && !individualCards.isEmpty();
        }

        public boolean selectRareFromSets() { return selectRareCards != null && !selectRareCards.isEmpty(); }

        public void addSelectedCard(final PaperCard card) {
            FModel.getQuest().getCards().addSingleCard(card, 1);
        }

    }

    public static final String UNDETERMINED = "quest_draft_undetermined_place";
    public static final String HUMAN = "quest_draft_human_place";
    public static final String DECK_NAME = "Tournament Deck";

    private static transient final ReadPriceList PRICE_LIST_READER = new ReadPriceList();
    private static transient final Map<String, Integer> MAP_PRICES = PRICE_LIST_READER.getPriceList();

    private String title = "Mystery Draft";
    private String boosterConfiguration = "";
    private String block = "";

    private int entryFee = 0;

    private String[] standings = new String[15];
    private String[] aiNames = new String[7];

    private int[] aiIcons = new int[7];

    private boolean started = false;
    private int age = FModel.getQuestPreferences().getPrefInt(QPref.WINS_NEW_DRAFT);

    public QuestEventDraft(final String title) {
        this.title = title;
    }

    public final String getTitle() {
        return this.title;
    }

    public final String[] getBoosterConfiguration() {
        return boosterConfiguration.split("/");
    }

    public final void setBoosterConfiguration(final String boosterConfiguration) {
        this.boosterConfiguration = boosterConfiguration;
    }

    public final void setEntryFee(final int entryFee) {
        this.entryFee = entryFee;
    }

    public final int getEntryFee() {
        return entryFee;
    }

    public final void setBlock(final String block) {
        this.block = block;
    }

    public final String getBlock() {
        return block;
    }

    public void setTitle(final String title0) {
        this.title = title0;
    }

    public void setStandings(final String[] standings) {
        this.standings = standings;
    }

    public String[] getStandings() {
        return standings;
    }

    public String[] getAINames() {
        return aiNames;
    }

    public void setAINames(final String[] names) {
        aiNames = names;
    }

    public int[] getAIIcons() {
        return aiIcons;
    }

    public void setAIIcons(final int[] icons) {
        aiIcons = icons;
    }

    public int getAge() {
        return age;
    }

    public void setAge(final int age) {
        this.age = age;
    }

    public void addWin() {
        age--;
    }

    public void saveToRegularDraft() {
        final String tournamentName = FModel.getQuest().getName() + " Tournament Deck " + new SimpleDateFormat("EEE d MMM yyyy HH-mm-ss").format(new Date());
        final DeckGroup original = FModel.getQuest().getDraftDecks().get(QuestEventDraft.DECK_NAME);
        final DeckGroup output = new DeckGroup(tournamentName);
        for (final Deck aiDeck : original.getAiDecks()) {
            output.addAiDeck(copyDeck(aiDeck));
        }
        output.setHumanDeck(copyDeck(original.getHumanDeck(), tournamentName));
        FModel.getDecks().getDraft().add(output);
    }

    public void addToQuestDecks() {
        final String deckName = "Tournament Deck " + new SimpleDateFormat("EEE d MMM yyyy HH-mm-ss").format(new Date());

        final Deck tournamentDeck = FModel.getQuest().getDraftDecks().get(QuestEventDraft.DECK_NAME).getHumanDeck();
        final Deck deck = new Deck(deckName);

        FModel.getQuest().getCards().addAllCards(tournamentDeck.getAllCardsInASinglePool().toFlatList());

        if (tournamentDeck.get(DeckSection.Main).countAll() > 0) {
            deck.getOrCreate(DeckSection.Main).addAll(tournamentDeck.get(DeckSection.Main));
            FModel.getQuest().getMyDecks().add(deck);
        }

        FModel.getQuest().getDraftDecks().delete(QuestEventDraft.DECK_NAME);
        FModel.getQuest().getAchievements().endCurrentTournament(FModel.getQuest().getAchievements().getCurrentDraft().getPlayerPlacement());
        FModel.getQuest().save();
    }

    private static Deck copyDeck(final Deck deck) {
        return new Deck(deck);
    }

    private static Deck copyDeck(final Deck deck, final String deckName) {
        return new Deck(deck, deckName);
    }

    public void setWinner(final String playerName) {

        boolean isHumanPlayer = true;

        for (final String name : aiNames) {
            if (playerName.equals(name)) {
                isHumanPlayer = false;
            }
        }

        int playerIndex = -1;

        if (isHumanPlayer) {
            for (int i = standings.length - 1; i >= 0; i--) {
                if (standings[i].equals(HUMAN)) {
                    playerIndex = i;
                    break;
                }
            }
        } else {

            String aiIndex = "";

            for (int i = aiNames.length - 1; i >= 0; i--) {
                if (aiNames[i].equals(playerName)) {
                    aiIndex = "" + (i + 1);
                    break;
                }
            }

            for (int i = standings.length - 1; i >= 0; i--) {
                if (standings[i].equals(aiIndex)) {
                    playerIndex = i;
                    break;
                }
            }

        }

        switch (playerIndex) {
        case 0:
        case 1:
            standings[8] = standings[playerIndex];
            break;
        case 2:
        case 3:
            standings[9] = standings[playerIndex];
            break;
        case 4:
        case 5:
            standings[10] = standings[playerIndex];
            break;
        case 6:
        case 7:
            standings[11] = standings[playerIndex];
            break;
        case 8:
        case 9:
            standings[12] = standings[playerIndex];
            break;
        case 10:
        case 11:
            standings[13] = standings[playerIndex];
            break;
        case 12:
        case 13:
            standings[14] = standings[playerIndex];
            break;
        }

    }

    /**
     * Generates the prizes for the player and saves them to the current quest.
     */
    public QuestDraftPrizes collectPrizes() {

        final int place = getPlayerPlacement();
        int prizePool = entryFee * 9;

        int boosterPrices = 0;

        for (final String boosterSet : boosterConfiguration.split("/")) {

            int value;
            final String boosterName = FModel.getMagicDb().getEditions().get(boosterSet).getName() + " Booster Pack";

            if (MAP_PRICES.containsKey(boosterName)) {
                value = MAP_PRICES.get(boosterName);
            } else {
                value = 395;
            }

            boosterPrices += value;

        }

        prizePool -= boosterPrices * 8;

        QuestDraftPrizes prizes = null;

        switch (place) {
        case 1:
            prizes = generateFirstPlacePrizes(prizePool);
            break;
        case 2:
            prizes = generateSecondPlacePrizes(prizePool);
            break;
        case 3:
            prizes = generateThirdPlacePrizes();
            break;
        case 4:
            prizes = generateFourthPlacePrizes();
            break;
        }

        if (prizes != null) {

            if (prizes.hasCredits()) {
                FModel.getQuest().getAssets().addCredits(prizes.credits);
            }

            if (prizes.hasBoosterPacks()) {
                for (final BoosterPack boosterPack : prizes.boosterPacks) {
                    FModel.getQuest().getCards().addAllCards(boosterPack.getCards());
                }
            }

            if (prizes.hasIndividualCards()) {
                FModel.getQuest().getCards().addAllCards(prizes.individualCards);
            }

            return prizes;

        }

        return null;

    }

    private QuestDraftPrizes generateFirstPlacePrizes(final int prizePool) {

        int credits = 2 * (prizePool / 3); //First place gets 2/3 the total prize pool
        final List<PaperCard> cards = new ArrayList<>();
        final List<BoosterPack> boosters = new ArrayList<>();

        cards.add(getPromoCard());

        int creditsLeftAfterPacks = generateBoosters((credits / 2), boosters); //Spend 75% of the credits on packs
        credits = (credits / 2) + creditsLeftAfterPacks; //Add the leftover credits + 50%

        final QuestDraftPrizes prizes = new QuestDraftPrizes();
        prizes.credits = credits;
        prizes.boosterPacks = boosters;
        prizes.individualCards = cards;
        awardSelectedRare(prizes);

        return prizes;

    }

    private QuestDraftPrizes generateSecondPlacePrizes(final int prizePool) {

        int credits = prizePool / 3; //Second place gets 1/3 the total prize pool
        final List<PaperCard> cards = new ArrayList<>();
        final List<BoosterPack> boosters = new ArrayList<>();

        cards.add(getPromoCard());

        int creditsLeftAfterPacks = generateBoosters((credits / 4) * 3, boosters); //Spend 75% of the credits on packs

        credits = (credits / 4) + creditsLeftAfterPacks; //Add the leftover credits + 25%

        final QuestDraftPrizes prizes = new QuestDraftPrizes();
        prizes.credits = credits;
        prizes.boosterPacks = boosters;
        prizes.individualCards = cards;
        awardSelectedRare(prizes);

        return prizes;

    }

    private QuestDraftPrizes generateThirdPlacePrizes() {

        final int credits = 0;
        final List<PaperCard> cards = new ArrayList<>();

        cards.add(getPromoCard());

        final List<BoosterPack> boosters = new ArrayList<>();
        boosters.add(getBoosterPack());

        final QuestDraftPrizes prizes = new QuestDraftPrizes();
        prizes.credits = credits;
        prizes.boosterPacks = boosters;
        prizes.individualCards = cards;

        return prizes;

    }

    private QuestDraftPrizes generateFourthPlacePrizes() {

        final int credits = 0;
        final List<PaperCard> cards = new ArrayList<>();

        cards.add(getPromoCard());

        final QuestDraftPrizes prizes = new QuestDraftPrizes();
        prizes.credits = credits;
        prizes.individualCards = cards;

        return prizes;

    }

    private int generateBoosters(final int creditsForPacks, final List<BoosterPack> boosters) {
        int creditsAfterPacks = creditsForPacks;
        while (true) {
            final BoosterPack pack = getBoosterPack();
            final int price = getBoosterPrice(pack);
            if (price > creditsAfterPacks * 1.1f) { //Add a little room for near-same price packs.
                break;
            }
            creditsAfterPacks -= price;
            boosters.add(pack);
        }
        return creditsAfterPacks;
    }

    private void awardSelectedRare(final QuestDraftPrizes prizes) {

        final List<PaperCard> possibleCards = new ArrayList<>();

        for (final CardEdition edition : getAllEditions()) {
            for (final CardInSet card : edition.getCards()) {
                if (card.rarity == CardRarity.Rare || card.rarity == CardRarity.MythicRare) {
                    final PaperCard cardToAdd = FModel.getMagicDb().getCommonCards().getCard(card.name, edition.getCode());
                    if (cardToAdd != null) {
                        possibleCards.add(cardToAdd);
                    }
                }
            }
        }

        prizes.selectRareCards = possibleCards;

    }

    private BoosterPack getBoosterPack() {
        return BoosterPack.FN_FROM_SET.apply(getRandomEdition());
    }

    private PaperCard getPromoCard() {

        final CardEdition randomEdition = getRandomEdition();
        final List<CardInSet> cardsInEdition = new ArrayList<>();

        for (final CardInSet card : randomEdition.getCards()) {
            if (card.rarity == CardRarity.Rare || card.rarity == CardRarity.MythicRare) {
                cardsInEdition.add(card);
            }
        }


        CardInSet randomCard;
        PaperCard promo = null;

        int attempts = 25;

        while (promo == null && attempts-- > 0) {
            randomCard = cardsInEdition.get((int) (Math.random() * cardsInEdition.size()));
            promo = FModel.getMagicDb().getCommonCards().getCard(randomCard.name, randomEdition.getCode());
        }

        if (promo == null) {
            return FModel.getQuest().getCards().addRandomRare();
        }

        return promo;

    }

    private CardEdition getRandomEdition() {

        final List<CardEdition> editions = new ArrayList<>();
        for (final String booster : boosterConfiguration.split("/")) {
            editions.add(FModel.getMagicDb().getEditions().get(booster));
        }

        return editions.get((int) (Math.random() * editions.size()));

    }

    private Set<CardEdition> getAllEditions() {

        final Set<CardEdition> editions = new HashSet<>();
        for (final String booster : boosterConfiguration.split("/")) {
            editions.add(FModel.getMagicDb().getEditions().get(booster));
        }

        return editions;

    }

    private static int getBoosterPrice(final BoosterPack booster) {
        int value;

        final String boosterName = booster.getName();

        if (MAP_PRICES.containsKey(boosterName)) {
            value = MAP_PRICES.get(boosterName);
        } else {
            value = 395;
        }

        return value;

    }

    public boolean playerHasMatchesLeft() {

        int playerIndex = -1;
        for (int i = standings.length - 1; i >= 0; i--) {
            if (standings[i].equals(HUMAN)) {
                playerIndex = i;
                break;
            }
        }

        int nextMatchIndex;

        switch (playerIndex) {

        case 0:
        case 1:
            nextMatchIndex = 8;
            break;
        case 2:
        case 3:
            nextMatchIndex = 9;
            break;
        case 4:
        case 5:
            nextMatchIndex = 10;
            break;
        case 6:
        case 7:
            nextMatchIndex = 11;
            break;
        case 8:
        case 9:
            nextMatchIndex = 12;
            break;
        case 10:
        case 11:
            nextMatchIndex = 13;
            break;
        case 12:
        case 13:
            nextMatchIndex = 14;
            break;
        case 14:
        default:
            nextMatchIndex = -1;
            break;

        }

        return nextMatchIndex != -1 && standings[nextMatchIndex].equals(UNDETERMINED);

    }

    public int getPlayerPlacement() {

        int playerIndex = -1;
        for (int i = standings.length - 1; i >= 0; i--) {
            if (standings[i].equals(HUMAN)) {
                playerIndex = i;
                break;
            }
        }

        if (playerIndex <= 7) {
            return 4;
        }

        if (playerIndex <= 11) {
            return 3;
        }

        if (playerIndex <= 13) {
            return 2;
        }

        if (playerIndex == 14) {
            return 1;
        }

        return -1;

    }

    public String getPlacementString() {

        final int place = getPlayerPlacement();

        String output;

        switch (place) {
        case 1:
            output = "first";
            break;
        case 2:
            output = "second";
            break;
        case 3:
            output = "third";
            break;
        case 4:
            output = "fourth";
            break;
        default:
            output = "ERROR";
            break;
        }

        return output;

    }

    public boolean canEnter() {
        final long creditsAvailable = FModel.getQuest().getAssets().getCredits();
        return creditsAvailable < getEntryFee();
    }

    public BoosterDraft enter() {
        FModel.getQuest().getAchievements().setCurrentDraft(this);
        FModel.getQuest().getAssets().subtractCredits(getEntryFee());
        return BoosterDraft.createDraft(LimitedPoolType.Block, FModel.getBlocks().get(getBlock()), getBoosterConfiguration());
    }

    public boolean isStarted() {
        return started;
    }

    public void start() {
        started = true;
        FModel.getQuest().save();
    }

    public void setStarted(final boolean started) {
        this.started = started;
    }

    @Override
    public String toString() {
        return title;
    }

    public static class QuestDraftFormat implements Comparable<QuestDraftFormat> {

        private CardEdition edition;
        private CardBlock block;

        public QuestDraftFormat(final CardEdition edition) {
            this.edition = edition;
        }

        public QuestDraftFormat(final CardBlock block) {
            this.block = block;
        }

        private boolean isSet() {
            return edition != null;
        }

        @Override
        public String toString() {
            if (edition != null) {
                return edition.getName() + " (" + edition.getCode() + ")";
            }
            String blockString = block.getName() + " (";
            List<CardEdition> sets = block.getSets();
            for (int i = 0; i < sets.size(); i++) {
                CardEdition cardEdition = sets.get(i);
                blockString += cardEdition.getCode();
                if (i < sets.size() - 1) {
                    blockString += ", ";
                }
            }
            blockString += ")";
            return blockString;
        }

        public String getName() {
            if (edition != null) {
                String blockName = "";
                if (block == null) {
                    
                    // try to determine the block name for this edition if it exists
                    // to avoid situations where the block name would be different from the first set name
                    // (e.g. Urza vs. Urza's Saga, Masques vs. Mercadian Masques etc.).
                    for (CardBlock cb : FModel.getBlocks()) {
                        if (cb.getSets().contains(edition)) {
                            blockName = cb.getName();
                        }
                    }
                }

                if (!blockName.isEmpty() && blockName != edition.getName()) {
                    return blockName;
                }

                return edition.getName();
            }

            return block.getName();
        }

        @Override
        public int compareTo(final QuestDraftFormat other) {
            return toString().compareToIgnoreCase(other.toString());
        }

    }

    private static List<CardEdition> getAllowedSets(final QuestController quest) {

        final List<CardEdition> allowedQuestSets = new ArrayList<>();

        if (quest.getFormat() != null) {

            final List<String> allowedSetCodes = quest.getFormat().getAllowedSetCodes();

            for (final String setCode : allowedSetCodes) {
                allowedQuestSets.add(FModel.getMagicDb().getEditions().get(setCode));
            }

        }

        return allowedQuestSets;

    }

    private static List<CardBlock> getBlocks() {

        final List<CardBlock> blocks = new ArrayList<>();
        final IStorage<CardBlock> storage = FModel.getBlocks();

        for (final CardBlock b : storage) {
            if (b.getCntBoostersDraft() > 0) {
                blocks.add(b);
            }
        }

        return blocks;

    }

    public static List<QuestDraftFormat> getAvailableFormats(final QuestController quest) {

        final List<CardEdition> allowedQuestSets = getAllowedSets(quest);
        final List<QuestDraftFormat> possibleFormats = new ArrayList<>();
        final List<CardBlock> blocks = getBlocks();

        List<String> singleSets = new ArrayList<>();
        if (!allowedQuestSets.isEmpty()) {
            for (final CardBlock block : blocks) {

                boolean blockAllowed = false;
                boolean largeSetUnlocked = false;
                int unlockedSets = 0;
                final boolean allBlocksSanctioned = quest.getFormat().getAllowedSetCodes().isEmpty();

                for (final CardEdition set : block.getSets()) {
                    if (!allowedQuestSets.contains(set) && !allBlocksSanctioned) {
                        continue;
                    }
                    unlockedSets++;
                    if (set.isLargeSet()) {
                        largeSetUnlocked = true;
                    }
                }

                //Allow partially unlocked blocks if they contain at least one large and one small unlocked set.
                if (largeSetUnlocked && unlockedSets > 1) {
                    blockAllowed = true;
                }

                if (largeSetUnlocked && block.getSets().size() == 1) {
                    blockAllowed = true;
                    singleSets.add(block.getSets().get(0).getCode());
                }

                if (blockAllowed) {
                    possibleFormats.add(new QuestDraftFormat(block));
                }

            }

            for (CardEdition allowedQuestSet : allowedQuestSets) {
                if (allowedQuestSet.isLargeSet() && !singleSets.contains(allowedQuestSet.getCode())) {
                    possibleFormats.add(new QuestDraftFormat(allowedQuestSet));
                }
            }

        } else {
            for (CardBlock block : blocks) {
                possibleFormats.add(new QuestDraftFormat(block));
                if (block.getSets().size() > 1) {
                    for (CardEdition edition : block.getSets()) {
                        if (edition.isLargeSet()) {
                            possibleFormats.add(new QuestDraftFormat(edition));
                        }
                    }
                }
            }
        }

        Collections.sort(possibleFormats);
        return possibleFormats;

    }

    /**
     * Generates a random draft event based on the provided quest's limitations.
     * @param quest The quest used to determine set availability.
     * @return The created draft or null in the event no draft could be created.
     */
    public static QuestEventDraft getRandomDraftOrNull(final QuestController quest) {

        final List<QuestDraftFormat> possibleFormats = getAvailableFormats(quest);

        if (possibleFormats.isEmpty()) {
            return null;
        }

        Collections.shuffle(possibleFormats);
        return getDraftOrNull(quest, possibleFormats.get(0));

    }

    /**
     * Generates a draft event based on the provided format.
     * @return The created draft or null in the event no draft could be created.
     */
    public static QuestEventDraft getDraftOrNull(final QuestController quest, final QuestDraftFormat format) {

        final QuestEventDraft event = new QuestEventDraft(format.getName());

        if (format.isSet()) {
            CardEdition edition = format.edition;
            String boosterConfiguration = "";
            for (int i = 0; i < 3; i++) {
                boosterConfiguration += edition.getCode();
                if (i != 2) {
                    boosterConfiguration += "/";
                }
                event.boosterConfiguration = boosterConfiguration;
            }
        } else {
            final List<String> possibleSetCombinations = new ArrayList<>(getSetCombos(quest, format.block));
            Collections.shuffle(possibleSetCombinations);
            event.boosterConfiguration = possibleSetCombinations.get(0);
        }

        event.block = format.getName();
        event.entryFee = calculateEntryFee(event.boosterConfiguration.split("/"));

        final List<String> players = new ArrayList<>();
        players.add(HUMAN);
        players.add("1");
        players.add("2");
        players.add("3");
        players.add("4");
        players.add("5");
        players.add("6");
        players.add("7");

        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i++) {
            event.standings[i] = players.get(i);
        }

        for (int i = 8; i < event.standings.length; i++) {
            event.standings[i] = UNDETERMINED;
        }

        final List<String> usedNames = new ArrayList<>();
        usedNames.add(GamePlayerUtil.getGuiPlayer().getName());

        for (int i = 0; i < 7; i++) {
            event.aiNames[i] = NameGenerator.getRandomName("Any", "Any", usedNames);
            usedNames.add(event.aiNames[i]);
        }

        final int numberOfIcons = GuiBase.getInterface().getAvatarCount();
        final List<Integer> usedIcons = new ArrayList<>();

        for (int i = 0; i < 7; i++) {

            int icon;
            int attempts = 50;

            do {
                icon = (int) Math.floor(Math.random() * numberOfIcons);
            } while ((icon < 0 || usedIcons.contains(icon)) && attempts-- > 0);

            event.aiIcons[i] = icon;
            usedNames.add(event.aiNames[i]);
            usedIcons.add(icon);

        }

        return event;

    }

    private static int calculateEntryFee(final String[] boosters) {
        int entryFee = 0;

        for (final String boosterSet : boosters) {

            int value;
            final String boosterName = FModel.getMagicDb().getEditions().get(boosterSet).getName() + " Booster Pack";

            if (MAP_PRICES.containsKey(boosterName)) {
                value = MAP_PRICES.get(boosterName);
            } else {
                value = 395;
            }

            entryFee += value;

        }

        return (int) (entryFee * 1.5);

    }

    private static Set<String> getSetCombos(final QuestController quest, final CardBlock block) {

        final Set<String> possibleCombinations = new LinkedHashSet<>();
        final List<CardEdition> sets = block.getSets();

        final String s0c = sets.get(0).getCode();
        if (sets.size() == 1) {
            int numBoosters = block.getCntBoostersDraft();
            String combination = "";
            for (int i = 0; i < numBoosters; i++) {
                combination += s0c;
                if (i < numBoosters - 1) {
                    combination += "/";
                }
            }
            possibleCombinations.add(combination);
            return possibleCombinations;
        }

        List<CardEdition> allowedSets;
        if (quest.getFormat() == null) {
            allowedSets = new ArrayList<>(sets);
        } else {
            allowedSets = getAllowedSets(quest);
            allowedSets.retainAll(sets);
        }

        final boolean oldSetsFirst = sets.get(0).getDate().before(FModel.getMagicDb().getEditions().get("SOM").getDate());
        Collections.sort(allowedSets, new Comparator<CardEdition>() {
            @Override
            public int compare(final CardEdition edition1, final CardEdition edition2) {
                if (edition1.getDate().before(edition2.getDate())) {
                    return oldSetsFirst ? -1 : 1;
                } else if (edition1.getDate().after(edition2.getDate())) {
                    return oldSetsFirst ? 1 : -1;
                }
                return 0;
            }
        });

        boolean largeSetFound = false;
        for (CardEdition allowedSet : allowedSets) {
            if (allowedSet.isLargeSet()) {
                largeSetFound = true;
                break;
            }
        }

        if (!largeSetFound) {
            throw new IllegalStateException(allowedSets + " does not contain a large set for quest draft generation.");
        }

        if (allowedSets.containsAll(sets)) {
            CardEdition set0 = allowedSets.get(0);
            CardEdition set1 = allowedSets.get(1);
            if (allowedSets.size() == 2) {
                final boolean draftOrder2016 = set0.getDate().after(FModel.getMagicDb().getEditions().get("BFZ").getDate()) || 
                        set1.getDate().after(FModel.getMagicDb().getEditions().get("BFZ").getDate());
;
                if (draftOrder2016) {
                    if (set0.isLargeSet()) {
                        possibleCombinations.add(String.format("%s/%s/%s", set1.getCode(), set1.getCode(), set0.getCode()));
                    } else {
                        possibleCombinations.add(String.format("%s/%s/%s", set0.getCode(), set0.getCode(), set1.getCode()));
                    }
                }
                else {
                    if (set0.isLargeSet()) {
                        possibleCombinations.add(String.format("%s/%s/%s", set0.getCode(), set0.getCode(), set1.getCode()));
                    } else {
                        possibleCombinations.add(String.format("%s/%s/%s", set0.getCode(), set1.getCode(), set1.getCode()));
                    }
                }
            }
            if (allowedSets.size() == 3) {
                CardEdition set2 = allowedSets.get(2);
                possibleCombinations.add(String.format("%s/%s/%s", set0.getCode(), set1.getCode(), set2.getCode()));
            }
        }

        return possibleCombinations;

    }

}
