using BepInEx;
using BepInEx.Logging;
using BepInEx.Configuration;
using AnimationManager;
using DiskCardGame;
using HarmonyLib;
using InscryptionAPI.Ascension;
using InscryptionAPI.Boons;
using InscryptionAPI.Card;
using InscryptionAPI.Helpers;
using InscryptionAPI.Nodes;
using InscryptionAPI.Regions;
using InscryptionAPI.Sound;
using InscryptionAPI.Triggers;
using InscryptionCommunityPatch.Card;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace BackToBeastSigils
{
    [BepInPlugin(PluginGuid, PluginName, PluginVersion)]
    [BepInDependency("cyantist.inscryption.api", BepInDependency.DependencyFlags.HardDependency)]
    [BepInDependency("cellri131.inscryption.animationmanager", BepInDependency.DependencyFlags.HardDependency)]
    public class Plugin : BaseUnityPlugin
    {
        private const string PluginGuid = "back.to.beast.sigils";
        private const string PluginName = "Back To Beast Sigils";
        private const string PluginVersion = "1.0.0";

        internal static ManualLogSource Log;

        private void Awake()
        {
            Log = Logger;

            Log.LogInfo($"Plugin {PluginName} is loaded!");

            // Ajouter les sigils Hand of the Beast et Hand of Nature
            AddHandSigils();
            new Harmony(PluginGuid).PatchAll();
            
            BoneLordRewards.RegisterAbility(BoneRevenge.Id, "back.to.beast.sigils.BoneRevengeReward");
            Log.LogInfo($"Special ability BoneRevenge ID: {BoneRevenge.Id}");
        }

        private void AddHandSigils()
        {
            // Créer le sigil Hand of the Beast
            AbilityInfo handOfBeastInfo = AbilityManager.New(
                PluginGuid,
                "Hand Of Beast", 
                "At the start of your turn, if this card is still in play, draw an additional card.",
                typeof(HandOfBeastAbility),
                TextureHelper.GetImageAsTexture("hand_of_the_beast.png")
            )
            .SetDefaultPart1Ability()
            .SetCanStack(false);

            handOfBeastInfo.powerLevel = 4;
            HandOfBeastAbility.ability = handOfBeastInfo.ability;

            Log.LogInfo("Hand of the Beast sigil has been added!");

            // Créer le sigil Hand of Nature
            AbilityInfo handOfNatureInfo = AbilityManager.New(
                PluginGuid,
                "Hand Of Nature", 
                "At the start of your turn, if this card is alive, add a squirrel to your hand.",
                typeof(HandOfNatureAbility),
                TextureHelper.GetImageAsTexture("hand_of_nature.png")
            )
            .SetDefaultPart1Ability()
            .SetCanStack(false);

            handOfNatureInfo.powerLevel = 3;
            HandOfNatureAbility.ability = handOfNatureInfo.ability;

            Log.LogInfo("Hand of Nature sigil has been added!");

            // Créer le sigil Strange Evolution
            AbilityInfo strangeEvolutionInfo = AbilityManager.New(
                PluginGuid,
                "Strange Evolution", 
                "At the end of the opponent's turn, if two forms are defined, transform randomly. Otherwise, gain +2 ATK or +4 HP. Triggers only once per combat.",
                typeof(StrangeEvolutionAbility),
                TextureHelper.GetImageAsTexture("strange_evolution.png")
            )
            .SetDefaultPart1Ability()
            .SetCanStack(false);

            strangeEvolutionInfo.powerLevel = 3;
            StrangeEvolutionAbility.ability = strangeEvolutionInfo.ability;

            Log.LogInfo("Strange Evolution sigil has been added!");

            // Créer le sigil Tribe Life
            AbilityInfo tribeLifeInfo = AbilityManager.New(
                PluginGuid,
                "Tribe Life", 
                "When this card dies, a card of the same tribe takes its place on the board.",
                typeof(TribeLifeAbility),
                TextureHelper.GetImageAsTexture("tribe_life.png")
            )
            .SetDefaultPart1Ability()
            .SetCanStack(false);

            tribeLifeInfo.powerLevel = 4;
            TribeLifeAbility.ability = tribeLifeInfo.ability;

            Log.LogInfo("Tribe Life sigil has been added!");

            AbilityInfo martyrInfo = AbilityManager.New(
                PluginGuid,
                "Martyr",
                "A card bearing this sigil cannot be sacrificed.",
                typeof(MartyrAbility),
                TextureHelper.GetImageAsTexture("martyr.png")
            )
            .SetDefaultPart1Ability()
            .SetCanStack(false);

            martyrInfo.powerLevel = -2;
            MartyrAbility.ability = martyrInfo.ability;

            Log.LogInfo("Martyr sigil has been added!");
        }
    }

    public class MartyrAbility : AbilityBehaviour
    {
        public static Ability ability;
        public override Ability Ability => ability;
    }

    [HarmonyPatch(typeof(PlayableCard), nameof(PlayableCard.CanBeSacrificed), MethodType.Getter)]
    internal static class MartyrCombatPatch
    {
        private static void Postfix(PlayableCard __instance, ref bool __result)
        {
            if (__result && __instance.HasAbility(MartyrAbility.ability))
                __result = false;
        }
    }

    [HarmonyPatch(typeof(CardMergeSequencer), "GetValidCardsForSacrifice")]
    internal static class MartyrMergePatch
    {
        private static void Postfix(List<CardInfo> __result)
        {
            __result.RemoveAll(card => card.HasAbility(MartyrAbility.ability));
        }
    }

    [HarmonyPatch(typeof(CardRemoveSequencer), "GetValidCards")]
    internal static class MartyrAltarCardsPatch
    {
        private static void Postfix(List<CardInfo> __result)
        {
            __result.RemoveAll(card => card.HasAbility(MartyrAbility.ability));
        }
    }

    [HarmonyPatch(typeof(CardRemoveSequencer), nameof(CardRemoveSequencer.RemoveSequence))]
    internal static class MartyrEmptyAltarPatch
    {
        private static bool Prefix(ref IEnumerator __result)
        {
            if (RunState.DeckList.Count == 0 || RunState.DeckList.Exists(card => !card.HasAbility(MartyrAbility.ability)))
                return true;

            __result = SkipAltar();
            return false;
        }

        private static IEnumerator SkipAltar()
        {
            yield return Singleton<TextDisplayer>.Instance.ShowUntilInput("None of your cards can be sacrificed here.");
            RunState.Run.bonelordPuzzleActive = false;
            Singleton<GameFlowManager>.Instance.TransitionToGameState(GameState.Map, null);
        }
    }

    // Classe qui définit le comportement du sigil Hand of the Beast
    public class HandOfBeastAbility : AbilityBehaviour
    {
        public static Ability ability;

        public override Ability Ability
        {
            get
            {
                return ability;
            }
        }

        // Se déclenche au début de chaque tour
        public override bool RespondsToUpkeep(bool playerUpkeep)
        {
            // Se déclenche seulement au début du tour du joueur si la carte est sur le plateau du joueur
            return playerUpkeep && base.Card.OnBoard && !base.Card.OpponentCard && !base.Card.Dead;
        }

        // L'effet : piocher une carte supplémentaire du deck principal
        public override IEnumerator OnUpkeep(bool playerUpkeep)
        {
            yield return base.PreSuccessfulTriggerSequence();

            Plugin.Log.LogInfo("Hand of the Beast: Triggering extra draw!");

            // Attendre un peu pour que la pioche normale se termine
            yield return new WaitForSeconds(0.5f);

            // Piocher une carte directement du deck
            if (Singleton<CardDrawPiles>.Instance.Deck.Cards.Count > 0)
            {
                Plugin.Log.LogInfo("Drawing extra card for Hand of the Beast!");
                yield return Singleton<CardDrawPiles>.Instance.DrawCardFromDeck();
            }

            yield return base.LearnAbility(0.1f);
            yield break;
        }
    }

    // Classe qui définit le comportement du sigil Hand of Nature
    public class HandOfNatureAbility : AbilityBehaviour
    {
        public static Ability ability;

        public override Ability Ability
        {
            get
            {
                return ability;
            }
        }

        // Se déclenche au début de chaque tour
        public override bool RespondsToUpkeep(bool playerUpkeep)
        {
            // Se déclenche seulement au début du tour du joueur si la carte est sur le plateau du joueur
            return playerUpkeep && base.Card.OnBoard && !base.Card.OpponentCard && !base.Card.Dead;
        }

        // L'effet : obtenir un écureuil dans la main
        public override IEnumerator OnUpkeep(bool playerUpkeep)
        {
            yield return base.PreSuccessfulTriggerSequence();

            Plugin.Log.LogInfo("Hand of Nature: Adding squirrel to hand!");

            // Attendre un peu pour que la pioche normale se termine
            yield return new WaitForSeconds(0.3f);

            // Créer un écureuil directement (ou utiliser la carte de la pile d'écureuil si modifiée)
            CardInfo squirrelCard = CardLoader.GetCardByName("Squirrel");
            if (squirrelCard != null)
            {
                Plugin.Log.LogInfo("Creating squirrel card for Hand of Nature!");
                PlayableCard playableSquirrel = CardSpawner.SpawnPlayableCard(squirrelCard);
                yield return Singleton<PlayerHand>.Instance.AddCardToHand(playableSquirrel, new Vector3(0f, 0f, 0f), 0f);
            }

            yield return base.LearnAbility(0.1f);
            yield break;
        }
    }

    // Classe qui définit le comportement du sigil Strange Evolution
    public class StrangeEvolutionAbility : AbilityBehaviour
    {
        public static Ability ability;
        internal const string EvolutionModId = "back.to.beast.sigils.StrangeEvolution";
        private bool hasActivated;

        public override Ability Ability
        {
            get
            {
                return ability;
            }
        }

        public override bool RespondsToTurnEnd(bool playerTurnEnd)
        {
            return !playerTurnEnd && base.Card.OnBoard && !base.Card.OpponentCard && !base.Card.Dead &&
                !hasActivated && !base.Card.TemporaryMods.Exists(mod => mod.singletonId == EvolutionModId);
        }

        // L'effet : évolution aléatoire ou buff stats
        public override IEnumerator OnTurnEnd(bool playerTurnEnd)
        {
            yield return base.PreSuccessfulTriggerSequence();

            Plugin.Log.LogInfo("Strange Evolution: Triggering effect!");
            hasActivated = true; // Marquer comme activé

            string evolution1 = null;
            string evolution2 = null;

            // Parser les évolutions depuis la description
            string description = base.Card.Info.description;
            
            if (!string.IsNullOrEmpty(description))
            {
                System.Text.RegularExpressions.Regex regex1 = new System.Text.RegularExpressions.Regex(@"\[evolution1:([^\]]+)\]");
                System.Text.RegularExpressions.Regex regex2 = new System.Text.RegularExpressions.Regex(@"\[evolution2:([^\]]+)\]");
                
                var match1 = regex1.Match(description);
                var match2 = regex2.Match(description);
                
                if (match1.Success) evolution1 = match1.Groups[1].Value;
                if (match2.Success) evolution2 = match2.Groups[1].Value;
                
                Plugin.Log.LogInfo($"Strange Evolution: Parsed from description - evolution1: {evolution1}, evolution2: {evolution2}");
            }

            // Vérifier si on a les deux évolutions définies
            if (!string.IsNullOrEmpty(evolution1) && !string.IsNullOrEmpty(evolution2))
            {
                // Mode évolution : choisir entre les deux cartes
                Plugin.Log.LogInfo("Strange Evolution: Evolution mode activated!");
                
                bool evolveToFirst = UnityEngine.Random.Range(0f, 1f) < 0.5f;
                string chosenEvolution = evolveToFirst ? evolution1 : evolution2;
                
                Plugin.Log.LogInfo($"Strange Evolution: Chosen evolution {chosenEvolution}");

                CardInfo evolutionCard = ScriptableObjectLoader<CardInfo>.AllData.Exists(card => card != null && card.name == chosenEvolution)
                    ? CardLoader.GetCardByName(chosenEvolution)
                    : null;
                
                if (evolutionCard != null)
                {
                    Plugin.Log.LogInfo($"Strange Evolution: Evolving into {evolutionCard.name}");

                    base.Card.AddTemporaryMod(new CardModificationInfo { singletonId = EvolutionModId });
                    NativeAnimations.PlayEvolutionFlip(base.Card);
                    yield return new WaitForSeconds(0.15f);
                    base.Card.SetInfo(evolutionCard);

                    Plugin.Log.LogInfo("Strange Evolution: Evolution completed!");
                }
                else
                {
                    Plugin.Log.LogError($"Strange Evolution: Evolution card '{chosenEvolution}' not found!");
                }
            }
            else
            {
                // Mode buff : 50% +2 attaque ou 50% +4 vie
                Plugin.Log.LogInfo("Strange Evolution: Buff mode activated (no valid evolutions found)!");
                
                bool buffAttack = UnityEngine.Random.Range(0f, 1f) < 0.5f;
                
                var boost = buffAttack ? new CardModificationInfo(2, 0) : new CardModificationInfo(0, 4);
                boost.singletonId = EvolutionModId;
                NativeAnimations.PlayEvolutionFlip(base.Card);
                yield return new WaitForSeconds(0.15f);
                base.Card.AddTemporaryMod(boost);
                
                Plugin.Log.LogInfo("Strange Evolution: Buff applied!");
            }

            yield return base.LearnAbility(0.1f);
            yield break;
        }

    }

    [HarmonyPatch(typeof(DrawCopyOnDeath), "get_CardToDrawTempMods")]
    internal static class StrangeEvolutionCopyPatch
    {
        private static void Postfix(DrawCopyOnDeath __instance, ref List<CardModificationInfo> __result)
        {
            foreach (CardModificationInfo mod in __instance.GetComponent<PlayableCard>().TemporaryMods)
            {
                if (mod.singletonId != StrangeEvolutionAbility.EvolutionModId)
                    continue;

                if (__result == null)
                    __result = new List<CardModificationInfo>();
                if (!__result.Exists(existing => existing.singletonId == mod.singletonId))
                    __result.Add((CardModificationInfo)mod.Clone());
            }
        }
    }

    // Classe qui définit le comportement du sigil Tribe Life
    public class TribeLifeAbility : AbilityBehaviour
    {
        public static Ability ability;

        public override Ability Ability
        {
            get
            {
                return ability;
            }
        }

        // Se déclenche quand la carte meurt
        public override bool RespondsToPreDeathAnimation(bool wasSacrifice)
        {
            return base.Card.OnBoard && !base.Card.OpponentCard;
        }

        // L'effet : remplacer par une carte de la même tribu
        public override IEnumerator OnPreDeathAnimation(bool wasSacrifice)
        {
            yield return base.PreSuccessfulTriggerSequence();

            Plugin.Log.LogInfo("Tribe Life: Card is dying, looking for replacement!");

            // Sauvegarder le slot actuel avant que la carte meure
            CardSlot currentSlot = base.Card.Slot;
            
            // Obtenir la tribu de la carte mourante
            Tribe cardTribe = base.Card.Info.tribes.Count > 0 ? base.Card.Info.tribes[0] : Tribe.None;
            
            Plugin.Log.LogInfo($"Tribe Life: Card tribe is {cardTribe}");

            // Attendre que l'animation de mort commence
            yield return new WaitForSeconds(0.2f);

            // Chercher une carte de remplacement
            CardInfo replacementCard = FindReplacementCard(cardTribe);


            if (replacementCard != null)
            {
                Plugin.Log.LogInfo($"Tribe Life: Found replacement card {replacementCard.name}");

                // Attendre un peu puis placer la nouvelle carte
                yield return new WaitForSeconds(0.5f);
                
                
                // Créer la nouvelle carte dans le slot
                yield return Singleton<BoardManager>.Instance.CreateCardInSlot(replacementCard, currentSlot, 0.15f, true);
                NativeAnimations.PlayEvolutionFlip(base.Card);
                yield return new WaitForSeconds(0.5f);
                
                Plugin.Log.LogInfo("Tribe Life: Replacement card placed successfully!");
            }
            else
            {
                Plugin.Log.LogWarning("Tribe Life: No replacement card found!");
            }

            yield return base.LearnAbility(0.1f);
            yield break;
        }

        private CardInfo FindReplacementCard(Tribe tribe)
        {
            Plugin.Log.LogInfo($"Tribe Life: Searching for replacement with tribe {tribe}");

            // Méthode 1 : Chercher dans le deck du joueur (et retirer la carte du deck)
            CardInfo deckCard = FindCardInDeck(tribe);
            if (deckCard != null)
            {
                Plugin.Log.LogInfo($"Tribe Life: Found card in deck: {deckCard.name} (removed from deck to avoid duplicates)");
                return deckCard;
            }

            // Méthode 2 : Chercher dans toutes les cartes du jeu (pas besoin de retirer)
            CardInfo gameCard = FindCardInAllCards(tribe);
            if (gameCard != null)
            {
                Plugin.Log.LogInfo($"Tribe Life: Found card in game collection: {gameCard.name} (no deck removal needed)");
                return gameCard;
            }

            Plugin.Log.LogWarning("Tribe Life: No replacement card found anywhere!");
            return null;
        }

        private CardInfo FindCardInDeck(Tribe tribe)
        {
            var deck = Singleton<CardDrawPiles>.Instance.Deck;
            if (deck != null && deck.Cards != null && deck.Cards.Count > 0)
            {
                // Créer une liste des cartes à parcourir pour éviter la modification pendant l'itération
                var deckCards = new System.Collections.Generic.List<CardInfo>(deck.Cards);
                
                foreach (var card in deckCards)
                {
                    if (tribe == Tribe.None)
                    {
                        // Pour les cartes sans tribu, chercher d'autres cartes sans tribu
                        if (card.tribes == null || card.tribes.Count == 0)
                        {
                            // IMPORTANT: Retirer la carte du deck pour éviter les doublons
                            deck.Cards.Remove(card);
                            Plugin.Log.LogInfo($"Tribe Life: Removed {card.name} from deck to avoid duplicates");
                            return card;
                        }
                    }
                    else
                    {
                        // Pour les cartes avec tribu, chercher la même tribu
                        if (card.tribes != null && card.tribes.Contains(tribe))
                        {
                            // IMPORTANT: Retirer la carte du deck pour éviter les doublons
                            deck.Cards.Remove(card);
                            Plugin.Log.LogInfo($"Tribe Life: Removed {card.name} from deck to avoid duplicates");
                            return card;
                        }
                    }
                }
            }
            return null;
        }

        private CardInfo FindCardInAllCards(Tribe tribe)
        {
            // Obtenir toutes les cartes disponibles dans le jeu
            var allCards = CardLoader.AllData;
            var validCards = new System.Collections.Generic.List<CardInfo>();

            foreach (var card in allCards)
            {
                // Ignorer les cartes spéciales, rares ou problématiques
                if (card == null || string.IsNullOrEmpty(card.name) || 
                    card.name.Contains("Empty") || card.name.Contains("Rare") ||
                    card.name.Contains("!") || card.name.Contains("CHOICE"))
                {
                    continue;
                }

                if (tribe == Tribe.None)
                {
                    // Chercher des cartes sans tribu
                    if (card.tribes == null || card.tribes.Count == 0)
                    {
                        validCards.Add(card);
                    }
                }
                else
                {
                    // Chercher des cartes de la même tribu
                    if (card.tribes != null && card.tribes.Contains(tribe))
                    {
                        validCards.Add(card);
                    }
                }
            }

            // Retourner une carte aléatoire parmi les valides
            if (validCards.Count > 0)
            {
                int randomIndex = UnityEngine.Random.Range(0, validCards.Count);
                return validCards[randomIndex];
            }

            return null;
        }

    }

    public class BoneRevenge : SpecialCardBehaviour
    {
        public static readonly SpecialTriggeredAbility Id =
            SpecialTriggeredAbilityManager.Add("back.to.beast.sigils", "BoneRevenge", typeof(BoneRevenge)).Id;
    }
}