import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  Flame,
  Plus,
  ArrowLeft,
  MoreVertical,
  Calendar,
  Clock,
  Heart,
  Bookmark,
  Share2,
  Trash2,
  Edit2,
  AlertTriangle,
  Download,
  Upload,
  CheckCircle2,
  ChevronRight,
  Shield,
  Smartphone,
  Code2,
  Info,
  Sliders,
  Play,
  Square,
  RotateCcw,
  Sparkles,
  Layers,
  Terminal,
  ExternalLink,
  Tag,
  Bell,
  Minimize2,
  Maximize2,
  FastForward,
  Bot,
  Zap,
  Activity,
  Compass,
  Eye,
  RefreshCw,
  AlertOctagon,
  Check
} from 'lucide-react';

export type PlatformType = 'TIKTOK' | 'INSTAGRAM' | 'YOUTUBE';

export interface Account {
  id: string;
  username: string;
  platform: PlatformType;
  nicheTag: string;
  notes: string;
  targetDailyMinutes: number; // default 30
  targetDays: number;         // default 5 (options 3, 5, 7)
  streakResetCount: number;
  isExplicitlyWarmedUp: boolean;
  createdAt: number;
}

export interface WarmUpSession {
  id: string;
  accountId: string;
  startTime: number;
  endTime: number;
  durationMinutes: number;
  likesCount: number;
  savesCount: number;
  dateString: string; // YYYY-MM-DD
}

export interface DailySummary {
  dateString: string;
  totalMinutes: number;
  totalSessions: number;
  totalLikes: number;
  totalSaves: number;
  isTargetMet: boolean;
}

export type WarmUpStatusType =
  | { type: 'NOT_STARTED' }
  | { type: 'WARMING_UP'; currentDay: number; targetDays: number; progress: number }
  | { type: 'WARMED_UP' };

export type SessionMood = 'LOW' | 'MEDIUM' | 'HIGH';

export interface AssistSettingsState {
  isEnabled: boolean;
  sessionMood: SessionMood;
  instantSkipProb: number;  // ~0.25
  quickGlanceProb: number;  // ~0.20
  partialWatchProb: number; // ~0.25
  fullWatchProb: number;    // ~0.20
  rewatchProb: number;      // ~0.10
  likeProbability: number;  // ~0.08
  saveProbability: number;  // ~0.03
  unlikeProbability: number;// ~0.01
  minGapVideos: number;     // 3-8
  dailyLikeCap: number;     // 20
  dailySaveCap: number;     // 8
  nicheKeywords: string;
}

const INITIAL_ACCOUNTS: Account[] = [
  {
    id: 'acc-1',
    username: '@techreviewer_pro',
    platform: 'TIKTOK',
    nicheTag: 'AI Tools, Productivity',
    notes: 'US physical SIM • Clean proxy',
    targetDailyMinutes: 30,
    targetDays: 5,
    streakResetCount: 0,
    isExplicitlyWarmedUp: false,
    createdAt: Date.now() - 3 * 86400000,
  },
  {
    id: 'acc-2',
    username: '@urban.street.fit',
    platform: 'INSTAGRAM',
    nicheTag: 'Streetwear, Calisthenics',
    notes: 'Secondary brand identity',
    targetDailyMinutes: 30,
    targetDays: 5,
    streakResetCount: 0,
    isExplicitlyWarmedUp: false,
    createdAt: Date.now() - 5 * 86400000,
  },
  {
    id: 'acc-3',
    username: '@deepdive_shorts',
    platform: 'YOUTUBE',
    nicheTag: 'Documentaries, Finance',
    notes: 'Clean dedicated Google account',
    targetDailyMinutes: 30,
    targetDays: 5,
    streakResetCount: 0,
    isExplicitlyWarmedUp: true,
    createdAt: Date.now() - 10 * 86400000,
  }
];

function getLocalDateString(date: Date = new Date()): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const INITIAL_SESSIONS: WarmUpSession[] = [
  {
    id: 'sess-1',
    accountId: 'acc-1',
    startTime: Date.now() - 2 * 86400000,
    endTime: Date.now() - 2 * 86400000 + 15 * 60000,
    durationMinutes: 15,
    likesCount: 2,
    savesCount: 1,
    dateString: getLocalDateString(new Date(Date.now() - 2 * 86400000))
  },
  {
    id: 'sess-2',
    accountId: 'acc-1',
    startTime: Date.now() - 2 * 86400000 + 3600000,
    endTime: Date.now() - 2 * 86400000 + 3600000 + 15 * 60000,
    durationMinutes: 15,
    likesCount: 1,
    savesCount: 0,
    dateString: getLocalDateString(new Date(Date.now() - 2 * 86400000))
  },
  {
    id: 'sess-3',
    accountId: 'acc-1',
    startTime: Date.now() - 1 * 86400000,
    endTime: Date.now() - 1 * 86400000 + 30 * 60000,
    durationMinutes: 30,
    likesCount: 4,
    savesCount: 2,
    dateString: getLocalDateString(new Date(Date.now() - 1 * 86400000))
  },
  ...Array.from({ length: 5 }).map((_, i) => ({
    id: `sess-yt-${i}`,
    accountId: 'acc-3',
    startTime: Date.now() - (7 - i) * 86400000,
    endTime: Date.now() - (7 - i) * 86400000 + 30 * 60000,
    durationMinutes: 35,
    likesCount: 3,
    savesCount: 1,
    dateString: getLocalDateString(new Date(Date.now() - (7 - i) * 86400000))
  }))
];

export default function App() {
  const [accounts, setAccounts] = useState<Account[]>(() => {
    const saved = localStorage.getItem('warmup_accounts');
    return saved ? JSON.parse(saved) : INITIAL_ACCOUNTS;
  });

  const [sessions, setSessions] = useState<WarmUpSession[]>(() => {
    const saved = localStorage.getItem('warmup_sessions');
    return saved ? JSON.parse(saved) : INITIAL_SESSIONS;
  });

  // Navigation State
  const [currentView, setCurrentView] = useState<'HOME' | 'PLATFORM' | 'DETAIL' | 'ROADMAP' | 'GRADLE' | 'ASSIST'>('HOME');
  const [selectedPlatform, setSelectedPlatform] = useState<PlatformType>('TIKTOK');
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);

  // Modals & Dialogs
  const [isAddAccountOpen, setIsAddAccountOpen] = useState(false);
  const [accountToEdit, setAccountToEdit] = useState<Account | null>(null);
  const [isLogSessionOpen, setIsLogSessionOpen] = useState(false);
  const [quickNotice, setQuickNotice] = useState<string | null>(null);
  const [isNotificationDialogOpen, setIsNotificationDialogOpen] = useState(false);

  // Stage 2: Floating Stopwatch Timer State
  const [activeTimerAccount, setActiveTimerAccount] = useState<Account | null>(null);
  const [timerSeconds, setTimerSeconds] = useState(0);
  const [timerLikes, setTimerLikes] = useState(0);
  const [timerSaves, setTimerSaves] = useState(0);
  const [isTimerMinimized, setIsTimerMinimized] = useState(false);
  const [isTurboSpeed, setIsTurboSpeed] = useState(false);
  const timerStartTimeRef = useRef<number>(0);

  // Stage 3: Assist Mode Human Behavior Settings State
  const [assistSettings, setAssistSettings] = useState<AssistSettingsState>(() => {
    const saved = localStorage.getItem('warmup_assist_settings');
    return saved ? JSON.parse(saved) : {
      isEnabled: true,
      sessionMood: 'MEDIUM',
      instantSkipProb: 0.25,
      quickGlanceProb: 0.20,
      partialWatchProb: 0.25,
      fullWatchProb: 0.20,
      rewatchProb: 0.10,
      likeProbability: 0.08,
      saveProbability: 0.03,
      unlikeProbability: 0.01,
      minGapVideos: 4,
      dailyLikeCap: 20,
      dailySaveCap: 8,
      nicheKeywords: 'Tech, AI, Gadgets, Productivity'
    };
  });

  // Stage 3 Live Simulator State
  const [isSimulatorRunning, setIsSimulatorRunning] = useState(false);
  const [simAccount, setSimAccount] = useState<Account>(accounts[0] || INITIAL_ACCOUNTS[0]);
  const [simVideoIndex, setSimVideoIndex] = useState(1);
  const [simCurrentReaction, setSimCurrentReaction] = useState<string>('Ready');
  const [simWatchSeconds, setSimWatchSeconds] = useState(0);
  const [simTargetSeconds, setSimTargetSeconds] = useState(0);
  const [simSwipeTrajectory, setSimSwipeTrajectory] = useState<string>('');
  const [simLastAction, setSimLastAction] = useState<string>('Feed initialized');
  const [simVideosSinceAction, setSimVideosSinceAction] = useState(4);
  const [simLikesInSession, setSimLikesInSession] = useState(0);
  const [simSavesInSession, setSimSavesInSession] = useState(0);
  const [simLogs, setSimLogs] = useState<Array<{ id: string; time: string; text: string; type: 'info' | 'like' | 'save' | 'skip' | 'alert' }>>([]);
  const [simSafetyHalted, setSimSafetyHalted] = useState<string | null>(null);

  // Form states
  const [formUsername, setFormUsername] = useState('');
  const [formNiche, setFormNiche] = useState('');
  const [formNotes, setFormNotes] = useState('');
  const [formTargetDays, setFormTargetDays] = useState(5);
  const [formTargetMins, setFormTargetMins] = useState(30);

  // Manual session form
  const [sessDuration, setSessDuration] = useState(15);
  const [sessLikes, setSessLikes] = useState(2);
  const [sessSaves, setSessSaves] = useState(1);
  const [sessDate, setSessDate] = useState(getLocalDateString());

  // Save to localStorage
  useEffect(() => {
    localStorage.setItem('warmup_accounts', JSON.stringify(accounts));
  }, [accounts]);

  useEffect(() => {
    localStorage.setItem('warmup_sessions', JSON.stringify(sessions));
  }, [sessions]);

  useEffect(() => {
    localStorage.setItem('warmup_assist_settings', JSON.stringify(assistSettings));
  }, [assistSettings]);

  // Real-time Timer Ticker
  useEffect(() => {
    let interval: any;
    if (activeTimerAccount) {
      interval = setInterval(() => {
        setTimerSeconds(prev => prev + (isTurboSpeed ? 60 : 1));
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [activeTimerAccount, isTurboSpeed]);

  const showNotification = (msg: string) => {
    setQuickNotice(msg);
    setTimeout(() => setQuickNotice(null), 3500);
  };

  // Live Assist Simulator Step Loop
  useEffect(() => {
    if (!isSimulatorRunning || simSafetyHalted) return;

    const timer = setInterval(() => {
      setSimWatchSeconds(prev => {
        if (prev < simTargetSeconds) {
          return prev + 1;
        } else {
          // Time up for this video -> perform human swipe & roll next video!
          triggerNextSimulatedVideo();
          return 0;
        }
      });
    }, 600);

    return () => clearInterval(timer);
  }, [isSimulatorRunning, simTargetSeconds, simSafetyHalted, simVideoIndex]);

  const triggerNextSimulatedVideo = () => {
    if (simSafetyHalted) return;

    const nextIndex = simVideoIndex + 1;
    setSimVideoIndex(nextIndex);

    // Roll reaction using weighted probabilities
    const r = Math.random();
    let reaction = 'Partial Watch';
    let targetSecs = 12;
    let isFullOrRewatch = false;

    if (r < assistSettings.instantSkipProb) {
      reaction = 'Instant Skip (~25%)';
      targetSecs = Math.floor(Math.random() * 2) + 2; // 2-3s
    } else if (r < assistSettings.instantSkipProb + assistSettings.quickGlanceProb) {
      reaction = 'Quick Glance (~20%)';
      targetSecs = Math.floor(Math.random() * 4) + 5; // 5-8s
    } else if (r < assistSettings.instantSkipProb + assistSettings.quickGlanceProb + assistSettings.partialWatchProb) {
      reaction = 'Partial Watch (~25%)';
      targetSecs = Math.floor(Math.random() * 6) + 10; // 10-15s
    } else if (r < assistSettings.instantSkipProb + assistSettings.quickGlanceProb + assistSettings.partialWatchProb + assistSettings.fullWatchProb) {
      reaction = 'Full Watch (~20%)';
      targetSecs = Math.floor(Math.random() * 6) + 18; // 18-23s
      isFullOrRewatch = true;
    } else {
      reaction = 'Rewatch (~10%)';
      targetSecs = Math.floor(Math.random() * 12) + 28; // 28-39s (loops 2x)
      isFullOrRewatch = true;
    }

    setSimCurrentReaction(reaction);
    setSimTargetSeconds(targetSecs);

    // Curved Bezier swipe path generation
    const speedMs = Math.floor(Math.random() * 300) + 200; // 200-500ms
    const curvature = Math.floor(Math.random() * 30) - 15;
    const swipeDesc = `Bézier swipe: duration ${speedMs}ms, curve offset ${curvature}px, distance 76% height`;
    setSimSwipeTrajectory(swipeDesc);

    // Decide Likes & Saves
    const moodMult = assistSettings.sessionMood === 'LOW' ? 0.3 : assistSettings.sessionMood === 'HIGH' ? 1.6 : 1.0;
    const currentGap = simVideosSinceAction + 1;
    let actionDone = 'No action';
    let actionType: 'info' | 'like' | 'save' | 'skip' = 'info';

    if (reaction.includes('Skip')) {
      actionType = 'skip';
      actionDone = `Swiped away after ${targetSecs}s. (Skip strictly forbids likes)`;
    } else if (isFullOrRewatch && currentGap >= assistSettings.minGapVideos && Math.random() < assistSettings.likeProbability * moodMult) {
      // Like!
      const isDoubleTap = Math.random() > 0.4;
      const likeDelay = (Math.random() * 2 + 1.2).toFixed(1);
      const unlikeCheck = Math.random() < assistSettings.unlikeProbability;

      setSimLikesInSession(prev => prev + 1);
      setSimVideosSinceAction(0);
      actionType = 'like';
      actionDone = `Liked via ${isDoubleTap ? 'double-tap' : 'heart button'} after ${likeDelay}s delay. ${unlikeCheck ? '(Triggered 1% unlike test!)' : ''}`;

      // Check Save
      if (Math.random() < assistSettings.saveProbability * 3) {
        setSimSavesInSession(prev => prev + 1);
        actionDone += ` • Bookmarked/Saved video.`;
      }
    } else {
      setSimVideosSinceAction(currentGap);
      actionDone = `Watched ${targetSecs}s (${reaction}). Quiet gap: ${currentGap} videos.`;
    }

    setSimLastAction(actionDone);
    const newLog = {
      id: `log-${Date.now()}`,
      time: new Date().toLocaleTimeString([], { hour12: false, minute: '2-digit', second: '2-digit' }),
      text: `Video #${nextIndex}: ${reaction} (${targetSecs}s) → ${actionDone}`,
      type: actionType
    };
    setSimLogs(prev => [newLog, ...prev.slice(0, 15)]);
  };

  const handleSimulateSafetyBlock = () => {
    setIsSimulatorRunning(false);
    const reason = "Suspicious activity detected: 'Security verification / Slide captcha' identified in active window.";
    setSimSafetyHalted(reason);
    const alertLog = {
      id: `log-${Date.now()}`,
      time: new Date().toLocaleTimeString([], { hour12: false, minute: '2-digit', second: '2-digit' }),
      text: `SAFETY HALT: Captcha / Action Blocked detected! All automated gestures killed instantly. High-priority notification raised.`,
      type: 'alert' as const
    };
    setSimLogs(prev => [alertLog, ...prev]);
    showNotification("Emergency safety halt triggered! Gestures aborted.");
  };

  // WarmUp Calculation per account
  const accountCalculations = useMemo(() => {
    const todayStr = getLocalDateString();
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = getLocalDateString(yesterday);

    const map = new Map<string, {
      account: Account;
      status: WarmUpStatusType;
      qualifyingDays: number;
      consecutiveDays: number;
      hasMissedDay: boolean;
      todayMinutes: number;
      totalMinutes: number;
      totalLikes: number;
      totalSaves: number;
      dailyList: DailySummary[];
    }>();

    accounts.forEach(acc => {
      const accSessions = sessions.filter(s => s.accountId === acc.id);
      const totalMinutes = accSessions.reduce((acc, s) => acc + s.durationMinutes, 0);
      const totalLikes = accSessions.reduce((acc, s) => acc + s.likesCount, 0);
      const totalSaves = accSessions.reduce((acc, s) => acc + s.savesCount, 0);

      const dateMap = new Map<string, WarmUpSession[]>();
      accSessions.forEach(s => {
        const list = dateMap.get(s.dateString) || [];
        list.push(s);
        dateMap.set(s.dateString, list);
      });

      const dailyList: DailySummary[] = [];
      dateMap.forEach((sList, dStr) => {
        const dMins = sList.reduce((sum, s) => sum + s.durationMinutes, 0);
        const dLikes = sList.reduce((sum, s) => sum + s.likesCount, 0);
        const dSaves = sList.reduce((sum, s) => sum + s.savesCount, 0);
        dailyList.push({
          dateString: dStr,
          totalMinutes: dMins,
          totalSessions: sList.length,
          totalLikes: dLikes,
          totalSaves: dSaves,
          isTargetMet: dMins >= acc.targetDailyMinutes
        });
      });

      dailyList.sort((a, b) => b.dateString.localeCompare(a.dateString));

      const qualifyingDays = dailyList.filter(d => d.isTargetMet).length;
      const todaySummary = dailyList.find(d => d.dateString === todayStr);
      const todayMinutes = todaySummary?.totalMinutes || 0;
      const todayTargetMet = todayMinutes >= acc.targetDailyMinutes;

      let consecutiveDays = 0;
      const targetMetDates = new Set(dailyList.filter(d => d.isTargetMet).map(d => d.dateString));

      let cur = new Date();
      if (!todayTargetMet) {
        cur.setDate(cur.getDate() - 1);
      }
      while (true) {
        const dStr = getLocalDateString(cur);
        if (targetMetDates.has(dStr)) {
          consecutiveDays++;
          cur.setDate(cur.getDate() - 1);
        } else {
          break;
        }
      }

      let hasMissedDay = false;
      const yesterdaySummary = dailyList.find(d => d.dateString === yesterdayStr);
      if (qualifyingDays > 0 && !todayTargetMet) {
        const hadActivityBeforeYesterday = dailyList.some(d => d.dateString < yesterdayStr);
        if (hadActivityBeforeYesterday && (!yesterdaySummary || !yesterdaySummary.isTargetMet)) {
          hasMissedDay = true;
        }
      }

      const effectiveCount = acc.streakResetCount > 0
        ? consecutiveDays
        : Math.max(consecutiveDays, hasMissedDay ? consecutiveDays : qualifyingDays);

      let status: WarmUpStatusType;
      if (acc.isExplicitlyWarmedUp || effectiveCount >= acc.targetDays) {
        status = { type: 'WARMED_UP' };
      } else if (effectiveCount > 0) {
        status = {
          type: 'WARMING_UP',
          currentDay: effectiveCount,
          targetDays: acc.targetDays,
          progress: Math.min(1, effectiveCount / acc.targetDays)
        };
      } else {
        status = { type: 'NOT_STARTED' };
      }

      map.set(acc.id, {
        account: acc,
        status,
        qualifyingDays,
        consecutiveDays,
        hasMissedDay,
        todayMinutes,
        totalMinutes,
        totalLikes,
        totalSaves,
        dailyList
      });
    });

    return map;
  }, [accounts, sessions]);

  // Live 20-min Forecast Calculation
  const liveForecast = useMemo(() => {
    const avgSecs = (assistSettings.instantSkipProb * 2) +
      (assistSettings.quickGlanceProb * 6.5) +
      (assistSettings.partialWatchProb * 13) +
      (assistSettings.fullWatchProb * 21) +
      (assistSettings.rewatchProb * 35) + 1.5;

    const totalVideos = Math.round(1200 / Math.max(3, avgSecs));
    const qualifying = totalVideos * (assistSettings.fullWatchProb + assistSettings.rewatchProb);
    const moodMultiplier = assistSettings.sessionMood === 'LOW' ? 0.35 : assistSettings.sessionMood === 'HIGH' ? 1.6 : 1.0;
    const expLikes = Math.min(assistSettings.dailyLikeCap, Math.round(qualifying * assistSettings.likeProbability * moodMultiplier));
    const expSaves = Math.min(assistSettings.dailySaveCap, Math.round(expLikes * assistSettings.saveProbability * 3.5));

    return {
      totalVideos,
      expLikes,
      expSaves,
      avgSecs: Math.round(avgSecs)
    };
  }, [assistSettings]);

  const selectedCalc = selectedAccountId ? accountCalculations.get(selectedAccountId) : null;
  const selectedSessions = useMemo(() => {
    if (!selectedAccountId) return [];
    return sessions.filter(s => s.accountId === selectedAccountId).sort((a, b) => b.startTime - a.startTime);
  }, [sessions, selectedAccountId]);

  const accountsNeedingTimeToday = useMemo(() => {
    return Array.from(accountCalculations.values()).filter(
      c => c.status.type !== 'WARMED_UP' && !c.todayMinutes || (c.todayMinutes < c.account.targetDailyMinutes && c.status.type !== 'WARMED_UP')
    );
  }, [accountCalculations]);

  const totalAccountsCount = accounts.length;
  const warmedCount = Array.from(accountCalculations.values()).filter(c => c.status.type === 'WARMED_UP').length;
  const inProgressCount = Array.from(accountCalculations.values()).filter(c => c.status.type === 'WARMING_UP').length;
  const notStartedCount = Array.from(accountCalculations.values()).filter(c => c.status.type === 'NOT_STARTED').length;

  const handleStartTimer = (account: Account) => {
    setActiveTimerAccount(account);
    setTimerSeconds(0);
    setTimerLikes(0);
    setTimerSaves(0);
    setIsTimerMinimized(false);
    timerStartTimeRef.current = Date.now();
    showNotification(`Floating timer started for ${account.username}`);
  };

  const handleStopAndSaveTimer = () => {
    if (!activeTimerAccount) return;
    const elapsedMins = Math.max(1, Math.round(timerSeconds / 60));
    const now = Date.now();
    const newSession: WarmUpSession = {
      id: `sess-timer-${now}`,
      accountId: activeTimerAccount.id,
      startTime: timerStartTimeRef.current,
      endTime: now,
      durationMinutes: elapsedMins,
      likesCount: timerLikes,
      savesCount: timerSaves,
      dateString: getLocalDateString()
    };
    setSessions(prev => [newSession, ...prev]);
    showNotification(`Saved ${elapsedMins}m warmup session for ${activeTimerAccount.username}!`);
    setActiveTimerAccount(null);
  };

  const handleCancelTimer = () => {
    setActiveTimerAccount(null);
    showNotification('Timer cancelled');
  };

  const handleOpenAddDialog = (platform?: PlatformType) => {
    setAccountToEdit(null);
    setFormUsername('');
    setFormNiche('');
    setFormNotes('');
    setFormTargetDays(5);
    setFormTargetMins(30);
    if (platform) setSelectedPlatform(platform);
    setIsAddAccountOpen(true);
  };

  const handleOpenEditDialog = (account: Account) => {
    setAccountToEdit(account);
    setFormUsername(account.username);
    setFormNiche(account.nicheTag);
    setFormNotes(account.notes);
    setFormTargetDays(account.targetDays);
    setFormTargetMins(account.targetDailyMinutes);
    setIsAddAccountOpen(true);
  };

  const handleSaveAccount = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formUsername.trim()) return;

    const formattedUsername = formUsername.trim().startsWith('@')
      ? formUsername.trim()
      : `@${formUsername.trim()}`;

    if (accountToEdit) {
      setAccounts(prev =>
        prev.map(a =>
          a.id === accountToEdit.id
            ? {
                ...a,
                username: formattedUsername,
                nicheTag: formNiche.trim(),
                notes: formNotes.trim(),
                targetDays: formTargetDays,
                targetDailyMinutes: formTargetMins
              }
            : a
        )
      );
      showNotification(`Updated ${formattedUsername}`);
    } else {
      const newAcc: Account = {
        id: `acc-${Date.now()}`,
        username: formattedUsername,
        platform: selectedPlatform,
        nicheTag: formNiche.trim(),
        notes: formNotes.trim(),
        targetDailyMinutes: formTargetMins,
        targetDays: formTargetDays,
        streakResetCount: 0,
        isExplicitlyWarmedUp: false,
        createdAt: Date.now()
      };
      setAccounts(prev => [newAcc, ...prev]);
      showNotification(`Added ${formattedUsername} to ${selectedPlatform}`);
    }
    setIsAddAccountOpen(false);
  };

  const handleDeleteAccount = (accId: string) => {
    const acc = accounts.find(a => a.id === accId);
    setAccounts(prev => prev.filter(a => a.id !== accId));
    setSessions(prev => prev.filter(s => s.accountId !== accId));
    if (activeTimerAccount?.id === accId) {
      setActiveTimerAccount(null);
    }
    if (selectedAccountId === accId) {
      setCurrentView('PLATFORM');
      setSelectedAccountId(null);
    }
    showNotification(`Deleted ${acc?.username || 'account'}`);
  };

  const handleAddManualSession = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAccountId) return;
    const now = Date.now();
    const newSession: WarmUpSession = {
      id: `sess-${now}`,
      accountId: selectedAccountId,
      startTime: now - sessDuration * 60000,
      endTime: now,
      durationMinutes: sessDuration,
      likesCount: sessLikes,
      savesCount: sessSaves,
      dateString: sessDate
    };
    setSessions(prev => [newSession, ...prev]);
    setIsLogSessionOpen(false);
    showNotification(`Logged ${sessDuration}m session`);
  };

  const handleDeleteSession = (sessionId: string) => {
    setSessions(prev => prev.filter(s => s.id !== sessionId));
    showNotification('Session removed');
  };

  const handleKeepStreak = () => {
    if (!selectedAccountId) return;
    setAccounts(prev =>
      prev.map(a => a.id === selectedAccountId ? { ...a, streakResetCount: 0 } : a)
    );
    showNotification('Streak preserved despite gap');
  };

  const handleResetStreak = () => {
    if (!selectedAccountId) return;
    setAccounts(prev =>
      prev.map(a => a.id === selectedAccountId ? { ...a, streakResetCount: a.streakResetCount + 1 } : a)
    );
    showNotification('Streak reset to Day 1');
  };

  const handleExportCsv = () => {
    let csv = 'id,username,platform,nicheTag,notes,targetDailyMinutes,targetDays,isWarmedUp\n';
    accounts.forEach(a => {
      const isWarmed = accountCalculations.get(a.id)?.status.type === 'WARMED_UP';
      csv += `"${a.id}","${a.username}","${a.platform}","${a.nicheTag}","${a.notes}",${a.targetDailyMinutes},${a.targetDays},${isWarmed}\n`;
    });
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `warmup_accounts_${getLocalDateString()}.csv`;
    link.click();
    showNotification('Exported accounts to CSV');
  };

  const handleImportCsv = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = evt => {
      const text = evt.target?.result as string;
      if (!text) return;
      const lines = text.split('\n');
      const newAccounts: Account[] = [];
      for (let i = 1; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;
        const parts = line.split(',').map(s => s.replace(/^"|"$/g, '').trim());
        if (parts.length >= 3) {
          const username = parts[1] || parts[0];
          const platform = (parts[2]?.toUpperCase() as PlatformType) || 'TIKTOK';
          const niche = parts[3] || '';
          const notes = parts[4] || '';
          const targetMins = parseInt(parts[5]) || 30;
          const targetDays = parseInt(parts[6]) || 5;
          newAccounts.push({
            id: `acc-imported-${Date.now()}-${i}`,
            username: username.startsWith('@') ? username : `@${username}`,
            platform: ['TIKTOK', 'INSTAGRAM', 'YOUTUBE'].includes(platform) ? platform : 'TIKTOK',
            nicheTag: niche,
            notes: notes,
            targetDailyMinutes: targetMins,
            targetDays: targetDays,
            streakResetCount: 0,
            isExplicitlyWarmedUp: parts[7] === 'true',
            createdAt: Date.now()
          });
        }
      }
      if (newAccounts.length > 0) {
        setAccounts(prev => [...newAccounts, ...prev]);
        showNotification(`Imported ${newAccounts.length} accounts`);
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  };

  const timerMins = Math.floor(timerSeconds / 60);
  const timerSecs = timerSeconds % 60;
  const formattedStopwatch = `${String(timerMins).padStart(2, '0')}:${String(timerSecs).padStart(2, '0')}`;
  const timerTargetMins = activeTimerAccount?.targetDailyMinutes || 30;
  const timerProgress = Math.min(1, timerMins / timerTargetMins);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-indigo-500/30 relative pb-28">
      {/* Toast Notification */}
      {quickNotice && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 bg-slate-800 border border-indigo-500/50 shadow-xl shadow-black/50 text-slate-100 px-4 py-2 rounded-xl text-sm font-medium flex items-center gap-2 animate-bounce">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{quickNotice}</span>
        </div>
      )}

      {/* Main Header */}
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-30">
        <div className="max-w-4xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 via-rose-500 to-amber-500 flex items-center justify-center shadow-lg shadow-rose-500/20">
              <Flame className="w-6 h-6 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-bold text-lg text-white leading-tight">WarmUp Manager</h1>
                <span className="text-[10px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  Stage 3 Active
                </span>
              </div>
              <p className="text-xs text-slate-400">Compose UI • Room • Accessibility Assist Engine</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentView('ASSIST')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition flex items-center gap-1.5 shadow-md ${
                currentView === 'ASSIST'
                  ? 'bg-gradient-to-r from-indigo-600 to-purple-600 text-white shadow-indigo-600/30'
                  : 'bg-indigo-950/60 border border-indigo-500/40 text-indigo-300 hover:bg-indigo-900/80'
              }`}
            >
              <Bot className="w-3.5 h-3.5" />
              <span>Assist Engine</span>
            </button>
            <button
              onClick={() => setCurrentView('ROADMAP')}
              className={`px-2.5 py-1.5 rounded-lg text-xs font-medium transition flex items-center gap-1 ${
                currentView === 'ROADMAP'
                  ? 'bg-indigo-600 text-white'
                  : 'bg-slate-800/80 text-slate-300 hover:bg-slate-700'
              }`}
            >
              <Sliders className="w-3.5 h-3.5" />
              <span>Stages</span>
            </button>
            <button
              onClick={() => setCurrentView('GRADLE')}
              className={`px-2.5 py-1.5 rounded-lg text-xs font-medium transition flex items-center gap-1 ${
                currentView === 'GRADLE'
                  ? 'bg-indigo-600 text-white'
                  : 'bg-slate-800/80 text-slate-300 hover:bg-slate-700'
              }`}
            >
              <Terminal className="w-3.5 h-3.5" />
              <span>APK</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-4xl mx-auto px-4 py-6 flex-1 w-full">
        {/* VIEW: HOME */}
        {currentView === 'HOME' && (
          <div className="space-y-6">
            {/* Top Dashboard Overview */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-base font-semibold text-white">Warmup Progress Overview</h2>
                  <p className="text-xs text-slate-400">Account status aggregated across TikTok, Instagram & YouTube</p>
                </div>
                <div className="flex items-center gap-2">
                  <label className="cursor-pointer px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-lg border border-slate-700 transition flex items-center gap-1.5">
                    <Upload className="w-3.5 h-3.5 text-slate-400" />
                    <span>Import CSV</span>
                    <input type="file" accept=".csv" className="hidden" onChange={handleImportCsv} />
                  </label>
                  <button
                    onClick={handleExportCsv}
                    className="px-2.5 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium rounded-lg border border-slate-700 transition flex items-center gap-1.5"
                  >
                    <Download className="w-3.5 h-3.5 text-slate-400" />
                    <span>Export CSV</span>
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
                <div className="bg-slate-800/60 border border-slate-700/50 rounded-xl p-3.5">
                  <p className="text-xs text-slate-400 font-medium">Total Accounts</p>
                  <p className="text-2xl font-bold text-white mt-1">{totalAccountsCount}</p>
                  <p className="text-[11px] text-slate-500 mt-1">Across 3 platforms</p>
                </div>

                <div className="bg-emerald-950/30 border border-emerald-500/20 rounded-xl p-3.5">
                  <p className="text-xs text-emerald-400 font-medium">Warmed Up</p>
                  <p className="text-2xl font-bold text-emerald-300 mt-1">{warmedCount}</p>
                  <p className="text-[11px] text-emerald-500/80 mt-1">Ready for high trust</p>
                </div>

                <div className="bg-amber-950/30 border border-amber-500/20 rounded-xl p-3.5">
                  <p className="text-xs text-amber-400 font-medium">Warming Up</p>
                  <p className="text-2xl font-bold text-amber-300 mt-1">{inProgressCount}</p>
                  <p className="text-[11px] text-amber-500/80 mt-1">Active daily streak</p>
                </div>

                <div className="bg-slate-800/40 border border-slate-700/40 rounded-xl p-3.5">
                  <p className="text-xs text-slate-400 font-medium">Not Started</p>
                  <p className="text-2xl font-bold text-slate-300 mt-1">{notStartedCount}</p>
                  <p className="text-[11px] text-slate-500 mt-1">Needs Day 1 warmup</p>
                </div>
              </div>
            </div>

            {/* STAGE 3 HIGHLIGHT BANNER: ASSIST MODE */}
            <div className="bg-gradient-to-r from-indigo-950/60 via-purple-950/50 to-slate-900 border border-indigo-500/40 rounded-2xl p-5 shadow-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-indigo-400" />
                  <h3 className="font-bold text-white text-sm uppercase tracking-wider">
                    Stage 3 Human Behavior Assist Engine Active
                  </h3>
                </div>
                <p className="text-xs text-slate-300 leading-relaxed max-w-xl">
                  Re-rolls reactions per video (Instant Skip 25%, Glance 20%, Partial 25%, Full 20%, Rewatch 10%), Bezier curve swipes with jitter, like delays, and auto-stops on captchas.
                </p>
              </div>

              <button
                onClick={() => setCurrentView('ASSIST')}
                className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold shadow-lg shadow-indigo-600/30 transition flex items-center gap-2 shrink-0"
              >
                <Bot className="w-4 h-4" />
                <span>Configure & Test Assist Engine</span>
              </button>
            </div>

            {/* TODAY'S WARMUP QUEUE */}
            <div className="bg-slate-900 border border-indigo-900/60 rounded-2xl p-5 shadow-lg space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Clock className="w-4 h-4 text-indigo-400" />
                  <h3 className="text-sm font-bold text-white uppercase tracking-wider">
                    Today's Warmup Queue ({accountsNeedingTimeToday.length} accounts need time)
                  </h3>
                </div>
                <button
                  onClick={() => setIsNotificationDialogOpen(true)}
                  className="text-xs text-amber-400 hover:text-amber-300 flex items-center gap-1 font-medium"
                >
                  <Bell className="w-3.5 h-3.5" />
                  <span>WorkManager Push Preview</span>
                </button>
              </div>

              {accountsNeedingTimeToday.length === 0 ? (
                <div className="bg-slate-950/60 border border-slate-800 rounded-xl p-4 text-center text-xs text-emerald-400 font-medium">
                  🎉 All active accounts have met their daily warmup target for today!
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
                  {accountsNeedingTimeToday.map(calc => {
                    const acc = calc.account;
                    const remainingMins = Math.max(0, acc.targetDailyMinutes - calc.todayMinutes);
                    const isCurrentTimer = activeTimerAccount?.id === acc.id;

                    return (
                      <div
                        key={acc.id}
                        className={`bg-slate-950/80 border rounded-xl p-3 flex items-center justify-between transition ${
                          isCurrentTimer
                            ? 'border-indigo-500 shadow-md shadow-indigo-500/20'
                            : 'border-slate-800 hover:border-slate-700'
                        }`}
                      >
                        <div className="space-y-0.5">
                          <div className="flex items-center gap-2">
                            <span
                              className={`w-2 h-2 rounded-full ${
                                acc.platform === 'TIKTOK'
                                  ? 'bg-rose-500'
                                  : acc.platform === 'INSTAGRAM'
                                  ? 'bg-pink-500'
                                  : 'bg-red-500'
                              }`}
                            />
                            <span className="font-bold text-sm text-white">{acc.username}</span>
                          </div>
                          <p className="text-[11px] text-slate-400">
                            {calc.todayMinutes}/{acc.targetDailyMinutes}m today •{' '}
                            <span className="text-amber-400 font-semibold">{remainingMins}m remaining</span>
                          </p>
                        </div>

                        <div className="flex items-center gap-1.5">
                          <button
                            onClick={() => {
                              setSimAccount(acc);
                              setCurrentView('ASSIST');
                            }}
                            className="px-2.5 py-1.5 rounded-lg bg-purple-600/20 text-purple-300 hover:bg-purple-600 hover:text-white transition text-xs font-semibold flex items-center gap-1"
                            title="Open in Assist Engine"
                          >
                            <Bot className="w-3.5 h-3.5" />
                            <span>Assist</span>
                          </button>
                          <button
                            onClick={() => handleStartTimer(acc)}
                            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
                              isCurrentTimer
                                ? 'bg-emerald-600 text-white animate-pulse'
                                : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-md shadow-indigo-600/20'
                            }`}
                          >
                            <Play className="w-3.5 h-3.5 fill-current" />
                            <span>{isCurrentTimer ? 'Running...' : 'Timer'}</span>
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Platform Cards Section */}
            <div>
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Platforms</h3>
                <span className="text-xs text-slate-500">Tap platform to view accounts</span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {(['TIKTOK', 'INSTAGRAM', 'YOUTUBE'] as PlatformType[]).map(platform => {
                  const platCalcs = Array.from(accountCalculations.values()).filter(c => c.account.platform === platform);
                  const pWarmed = platCalcs.filter(c => c.status.type === 'WARMED_UP').length;
                  const pProgress = platCalcs.filter(c => c.status.type === 'WARMING_UP').length;
                  const pNotStarted = platCalcs.filter(c => c.status.type === 'NOT_STARTED').length;

                  return (
                    <div
                      key={platform}
                      onClick={() => {
                        setSelectedPlatform(platform);
                        setCurrentView('PLATFORM');
                      }}
                      className="bg-slate-900 border border-slate-800 rounded-2xl p-5 cursor-pointer transition transform hover:-translate-y-1 shadow-lg hover:border-slate-700 group"
                    >
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm bg-slate-800 text-white">
                            {platform === 'TIKTOK' && 'TT'}
                            {platform === 'INSTAGRAM' && 'IG'}
                            {platform === 'YOUTUBE' && 'YT'}
                          </div>
                          <div>
                            <h4 className="font-bold text-white text-base group-hover:text-indigo-300 transition">
                              {platform === 'TIKTOK' && 'TikTok'}
                              {platform === 'INSTAGRAM' && 'Instagram'}
                              {platform === 'YOUTUBE' && 'YouTube'}
                            </h4>
                            <p className="text-xs text-slate-400">{platCalcs.length} accounts tracked</p>
                          </div>
                        </div>
                        <ChevronRight className="w-5 h-5 text-slate-600 group-hover:text-slate-300 transition" />
                      </div>

                      <div className="grid grid-cols-3 gap-2 py-2 px-3 bg-slate-950/60 rounded-xl border border-slate-800/80 text-center">
                        <div>
                          <p className="text-xs text-emerald-400 font-bold">{pWarmed}</p>
                          <p className="text-[10px] text-slate-400">Warmed</p>
                        </div>
                        <div className="border-x border-slate-800">
                          <p className="text-xs text-amber-400 font-bold">{pProgress}</p>
                          <p className="text-[10px] text-slate-400">In Progress</p>
                        </div>
                        <div>
                          <p className="text-xs text-slate-400 font-bold">{pNotStarted}</p>
                          <p className="text-[10px] text-slate-400">Not Started</p>
                        </div>
                      </div>

                      <div className="mt-4 flex items-center justify-between pt-2 border-t border-slate-800/60 text-xs text-slate-400">
                        <span>Target: 30 min / 5 days</span>
                        <span className="text-indigo-400 font-medium group-hover:underline">Open List →</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {/* VIEW: STAGE 3 ASSIST MODE & LIVE BEHAVIOR SIMULATOR */}
        {currentView === 'ASSIST' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setCurrentView('HOME')}
                  className="p-2 bg-slate-900 border border-slate-800 rounded-xl text-slate-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <div>
                  <h2 className="text-xl font-bold text-white flex items-center gap-2">
                    <Bot className="w-5 h-5 text-indigo-400" />
                    <span>Assist Mode: Human Behavior Engine</span>
                  </h2>
                  <p className="text-xs text-slate-400">
                    AccessibilityService gesture dispatcher with randomized organic timing and safety traps
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-slate-400">Assist Mode</span>
                <button
                  onClick={() => {
                    setAssistSettings(s => ({ ...s, isEnabled: !s.isEnabled }));
                    showNotification(`Assist mode ${!assistSettings.isEnabled ? 'ENABLED' : 'DISABLED'}`);
                  }}
                  className={`w-12 h-6 flex items-center rounded-full p-1 cursor-pointer transition ${
                    assistSettings.isEnabled ? 'bg-indigo-600 justify-end' : 'bg-slate-800 justify-start'
                  }`}
                >
                  <div className="bg-white w-4 h-4 rounded-full shadow-md" />
                </button>
              </div>
            </div>

            {/* Live 20-Min Session Forecast Card */}
            <div className="bg-gradient-to-r from-slate-900 to-indigo-950/40 border border-indigo-500/30 rounded-2xl p-5 shadow-lg space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-sky-400" />
                  <h3 className="text-sm font-bold text-sky-300 uppercase tracking-wider">
                    Live 20-Minute Session Forecast (From Your Sliders)
                  </h3>
                </div>
                <span className="text-xs text-slate-400">Mood: {assistSettings.sessionMood}</span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
                <div className="bg-slate-950/80 p-3 rounded-xl border border-slate-800">
                  <p className="text-2xl font-bold text-white">~{liveForecast.totalVideos}</p>
                  <p className="text-xs text-slate-400 mt-0.5">Videos Scrolled</p>
                  <p className="text-[10px] text-slate-500">avg {liveForecast.avgSecs}s/video</p>
                </div>
                <div className="bg-slate-950/80 p-3 rounded-xl border border-slate-800">
                  <p className="text-2xl font-bold text-rose-400">~{liveForecast.expLikes}</p>
                  <p className="text-xs text-slate-400 mt-0.5">Expected Likes</p>
                  <p className="text-[10px] text-slate-500">6–10% of full/rewatches</p>
                </div>
                <div className="bg-slate-950/80 p-3 rounded-xl border border-slate-800">
                  <p className="text-2xl font-bold text-amber-400">~{liveForecast.expSaves}</p>
                  <p className="text-xs text-slate-400 mt-0.5">Expected Saves</p>
                  <p className="text-[10px] text-slate-500">2–4% after likes</p>
                </div>
                <div className="bg-slate-950/80 p-3 rounded-xl border border-slate-800">
                  <p className="text-2xl font-bold text-emerald-400">0%</p>
                  <p className="text-xs text-slate-400 mt-0.5">Robotic Pattern</p>
                  <p className="text-[10px] text-slate-500">Dynamic Bezier + Jitter</p>
                </div>
              </div>
            </div>

            {/* Interactive Live Feed Simulator */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800">
                <div>
                  <h3 className="text-sm font-bold text-white flex items-center gap-2">
                    <Activity className="w-4 h-4 text-emerald-400" />
                    <span>Real-Time Behavioral Simulator</span>
                  </h3>
                  <p className="text-xs text-slate-400">
                    Watch the Human Behavior Engine roll reactions, calculate Bézier swipe curves, and enforce quiet gaps in real time.
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => {
                      if (simSafetyHalted) {
                        setSimSafetyHalted(null);
                      }
                      if (!isSimulatorRunning) {
                        setIsSimulatorRunning(true);
                        triggerNextSimulatedVideo();
                      } else {
                        setIsSimulatorRunning(false);
                      }
                    }}
                    className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${
                      isSimulatorRunning
                        ? 'bg-amber-600 hover:bg-amber-500 text-white'
                        : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-600/30'
                    }`}
                  >
                    {isSimulatorRunning ? <Square className="w-3.5 h-3.5 fill-current" /> : <Play className="w-3.5 h-3.5 fill-current" />}
                    <span>{isSimulatorRunning ? 'Pause Simulator' : 'Start Feed Simulation'}</span>
                  </button>

                  <button
                    onClick={handleSimulateSafetyBlock}
                    className="px-3 py-2 bg-rose-950/60 hover:bg-rose-950 border border-rose-500/40 text-rose-300 rounded-xl text-xs font-bold transition flex items-center gap-1.5"
                    title="Simulate Captcha / Action Blocked trigger"
                  >
                    <AlertOctagon className="w-3.5 h-3.5" />
                    <span>Test Captcha Tripwire</span>
                  </button>
                </div>
              </div>

              {/* Safety Alert Notification if tripped */}
              {simSafetyHalted && (
                <div className="bg-rose-950/50 border border-rose-500/50 rounded-xl p-4 flex items-start gap-3">
                  <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                  <div>
                    <h4 className="text-xs font-bold text-rose-300 uppercase tracking-wider">
                      Emergency Safety Halt Tripped!
                    </h4>
                    <p className="text-xs text-slate-200 mt-1">
                      {simSafetyHalted} All gestures terminated immediately to protect account standing. Complete verification manually on your device.
                    </p>
                    <button
                      onClick={() => setSimSafetyHalted(null)}
                      className="mt-2 px-3 py-1 bg-rose-600 hover:bg-rose-500 text-white text-xs font-semibold rounded-lg"
                    >
                      Reset Tripwire
                    </button>
                  </div>
                </div>
              )}

              {/* Live Active Feed Card Simulation */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Mock Phone Feed Player */}
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 flex flex-col justify-between h-64 relative overflow-hidden">
                  <div className="flex items-center justify-between text-xs text-slate-400 z-10">
                    <span className="font-bold text-white flex items-center gap-1.5">
                      <Smartphone className="w-3.5 h-3.5 text-indigo-400" />
                      <span>Video #{simVideoIndex}</span>
                    </span>
                    <span className="px-2 py-0.5 rounded bg-slate-800 text-[10px] text-slate-300">
                      Target: {simAccount.platform}
                    </span>
                  </div>

                  <div className="my-auto text-center space-y-2 z-10">
                    <span className="inline-block px-3 py-1 rounded-full bg-indigo-500/20 text-indigo-300 text-xs font-bold border border-indigo-500/40">
                      {simCurrentReaction}
                    </span>
                    <div className="text-2xl font-black font-mono text-white">
                      {simWatchSeconds}s / {simTargetSeconds}s
                    </div>
                    <div className="w-48 mx-auto h-1.5 bg-slate-800 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-indigo-500 transition-all"
                        style={{ width: `${Math.min(100, (simWatchSeconds / Math.max(1, simTargetSeconds)) * 100)}%` }}
                      />
                    </div>
                    <p className="text-[11px] text-slate-400 max-w-xs mx-auto">{simLastAction}</p>
                  </div>

                  {simSwipeTrajectory && (
                    <div className="text-[10px] font-mono text-slate-500 truncate z-10">
                      Kinetics: {simSwipeTrajectory}
                    </div>
                  )}

                  {/* Subtle simulated gesture line animation */}
                  <div className="absolute inset-0 pointer-events-none opacity-20 bg-[radial-gradient(#6366f1_1px,transparent_1px)] [background-size:16px_16px]" />
                </div>

                {/* Live Real-Time Action Log */}
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 flex flex-col h-64">
                  <div className="flex items-center justify-between text-xs pb-2 border-b border-slate-800 text-slate-400">
                    <span className="font-bold text-white">Behavioral Telemetry Log</span>
                    <span>Session: {simLikesInSession} likes • {simSavesInSession} saves</span>
                  </div>

                  <div className="flex-1 overflow-y-auto space-y-2 pt-2 text-[11px] font-mono">
                    {simLogs.length === 0 ? (
                      <p className="text-slate-600 text-center py-10">Click 'Start Feed Simulation' to begin tracking events...</p>
                    ) : (
                      simLogs.map(log => (
                        <div
                          key={log.id}
                          className={`p-2 rounded-lg border ${
                            log.type === 'like'
                              ? 'bg-rose-950/30 border-rose-500/40 text-rose-200'
                              : log.type === 'alert'
                              ? 'bg-red-950/70 border-red-500 text-red-200 font-bold'
                              : log.type === 'skip'
                              ? 'bg-slate-900 border-slate-800 text-slate-400'
                              : 'bg-slate-900/60 border-slate-800 text-slate-300'
                          }`}
                        >
                          <span className="text-slate-500 mr-2">[{log.time}]</span>
                          <span>{log.text}</span>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Settings & Sliders Configuration */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              {/* Sliders: Viewing Reactions Distribution */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-lg">
                <h3 className="font-bold text-sm text-white uppercase tracking-wider flex items-center gap-2">
                  <Eye className="w-4 h-4 text-indigo-400" />
                  <span>Viewing Reaction Weights</span>
                </h3>

                <div className="space-y-3 text-xs">
                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-slate-300 font-medium">Instant Skip (1–3s)</span>
                      <span className="text-indigo-400 font-bold">{Math.round(assistSettings.instantSkipProb * 100)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.05"
                      max="0.45"
                      step="0.05"
                      value={assistSettings.instantSkipProb}
                      onChange={e => setAssistSettings(s => ({ ...s, instantSkipProb: parseFloat(e.target.value) }))}
                      className="w-full accent-indigo-500 bg-slate-800"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-slate-300 font-medium">Quick Glance (20–40%)</span>
                      <span className="text-indigo-400 font-bold">{Math.round(assistSettings.quickGlanceProb * 100)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.05"
                      max="0.40"
                      step="0.05"
                      value={assistSettings.quickGlanceProb}
                      onChange={e => setAssistSettings(s => ({ ...s, quickGlanceProb: parseFloat(e.target.value) }))}
                      className="w-full accent-indigo-500 bg-slate-800"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-slate-300 font-medium">Partial Watch (50–80%)</span>
                      <span className="text-indigo-400 font-bold">{Math.round(assistSettings.partialWatchProb * 100)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.05"
                      max="0.45"
                      step="0.05"
                      value={assistSettings.partialWatchProb}
                      onChange={e => setAssistSettings(s => ({ ...s, partialWatchProb: parseFloat(e.target.value) }))}
                      className="w-full accent-indigo-500 bg-slate-800"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-slate-300 font-medium">Full Watch (end + loop)</span>
                      <span className="text-indigo-400 font-bold">{Math.round(assistSettings.fullWatchProb * 100)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.05"
                      max="0.35"
                      step="0.05"
                      value={assistSettings.fullWatchProb}
                      onChange={e => setAssistSettings(s => ({ ...s, fullWatchProb: parseFloat(e.target.value) }))}
                      className="w-full accent-indigo-500 bg-slate-800"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-slate-300 font-medium">Rewatch (loops 2–3x)</span>
                      <span className="text-indigo-400 font-bold">{Math.round(assistSettings.rewatchProb * 100)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.02"
                      max="0.25"
                      step="0.02"
                      value={assistSettings.rewatchProb}
                      onChange={e => setAssistSettings(s => ({ ...s, rewatchProb: parseFloat(e.target.value) }))}
                      className="w-full accent-indigo-500 bg-slate-800"
                    />
                  </div>
                </div>
              </div>

              {/* Engagement Sliders & Mood */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-lg">
                <h3 className="font-bold text-sm text-white uppercase tracking-wider flex items-center gap-2">
                  <Zap className="w-4 h-4 text-amber-400" />
                  <span>Engagement & Safety Caps</span>
                </h3>

                {/* Session Mood Picker */}
                <div className="space-y-1.5">
                  <label className="text-xs text-slate-400 font-medium">Session Mood Scaling</label>
                  <div className="grid grid-cols-3 gap-2">
                    {(['LOW', 'MEDIUM', 'HIGH'] as SessionMood[]).map(m => (
                      <button
                        key={m}
                        onClick={() => setAssistSettings(s => ({ ...s, sessionMood: m }))}
                        className={`py-2 rounded-xl text-xs font-bold border transition ${
                          assistSettings.sessionMood === m
                            ? 'bg-indigo-600 border-indigo-500 text-white shadow-md'
                            : 'bg-slate-950 border-slate-800 text-slate-400 hover:bg-slate-800'
                        }`}
                      >
                        {m}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="space-y-3 text-xs pt-1">
                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-slate-300 font-medium">Like Probability (6–10%)</span>
                      <span className="text-rose-400 font-bold">{Math.round(assistSettings.likeProbability * 100)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.02"
                      max="0.15"
                      step="0.01"
                      value={assistSettings.likeProbability}
                      onChange={e => setAssistSettings(s => ({ ...s, likeProbability: parseFloat(e.target.value) }))}
                      className="w-full accent-rose-500 bg-slate-800"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between mb-1">
                      <span className="text-slate-300 font-medium">Save Probability (2–4%)</span>
                      <span className="text-amber-400 font-bold">{Math.round(assistSettings.saveProbability * 100)}%</span>
                    </div>
                    <input
                      type="range"
                      min="0.01"
                      max="0.08"
                      step="0.01"
                      value={assistSettings.saveProbability}
                      onChange={e => setAssistSettings(s => ({ ...s, saveProbability: parseFloat(e.target.value) }))}
                      className="w-full accent-amber-500 bg-slate-800"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-3 pt-1">
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Daily Like Cap</label>
                      <input
                        type="number"
                        value={assistSettings.dailyLikeCap}
                        onChange={e => setAssistSettings(s => ({ ...s, dailyLikeCap: parseInt(e.target.value) || 10 }))}
                        className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] text-slate-400 mb-1">Daily Save Cap</label>
                      <input
                        type="number"
                        value={assistSettings.dailySaveCap}
                        onChange={e => setAssistSettings(s => ({ ...s, dailySaveCap: parseInt(e.target.value) || 5 }))}
                        className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-white"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* VIEW: PLATFORM ACCOUNT LIST */}
        {currentView === 'PLATFORM' && (
          <div className="space-y-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setCurrentView('HOME')}
                  className="p-2 bg-slate-900 border border-slate-800 rounded-xl text-slate-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <div>
                  <h2 className="text-xl font-bold text-white capitalize">
                    {selectedPlatform.toLowerCase()} Accounts
                  </h2>
                  <p className="text-xs text-slate-400">
                    {Array.from(accountCalculations.values()).filter(c => c.account.platform === selectedPlatform).length} accounts registered
                  </p>
                </div>
              </div>

              <button
                onClick={() => handleOpenAddDialog(selectedPlatform)}
                className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-indigo-600/20 transition flex items-center gap-1.5"
              >
                <Plus className="w-4 h-4" />
                <span>Add Account</span>
              </button>
            </div>

            {(() => {
              const platformCalcs = Array.from(accountCalculations.values()).filter(
                c => c.account.platform === selectedPlatform
              );

              if (platformCalcs.length === 0) {
                return (
                  <div className="bg-slate-900 border border-slate-800 rounded-2xl p-12 text-center space-y-3">
                    <Smartphone className="w-10 h-10 text-slate-600 mx-auto" />
                    <h3 className="text-base font-semibold text-slate-300">No {selectedPlatform} accounts yet</h3>
                    <p className="text-xs text-slate-500 max-w-sm mx-auto">
                      Add an account with your username, niche tag, and target warmup days to start tracking your daily minutes.
                    </p>
                    <button
                      onClick={() => handleOpenAddDialog(selectedPlatform)}
                      className="mt-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-xl border border-slate-700 transition inline-flex items-center gap-1.5"
                    >
                      <Plus className="w-4 h-4" />
                      <span>Add First Account</span>
                    </button>
                  </div>
                );
              }

              return (
                <div className="space-y-3">
                  {platformCalcs.map(calc => {
                    const acc = calc.account;
                    const dateAdded = new Date(acc.createdAt).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric'
                    });
                    const isRunningTimer = activeTimerAccount?.id === acc.id;

                    return (
                      <div
                        key={acc.id}
                        className={`bg-slate-900 border rounded-2xl p-4 transition shadow-md group ${
                          isRunningTimer ? 'border-indigo-500/80 shadow-indigo-500/10' : 'border-slate-800 hover:border-slate-700'
                        }`}
                      >
                        <div className="flex items-start justify-between">
                          <div
                            onClick={() => {
                              setSelectedAccountId(acc.id);
                              setCurrentView('DETAIL');
                            }}
                            className="cursor-pointer flex-1"
                          >
                            <div className="flex items-center gap-2">
                              <h3 className="font-bold text-base text-white group-hover:text-indigo-300 transition">
                                {acc.username}
                              </h3>
                              <span className="text-[11px] text-slate-500">Added {dateAdded}</span>
                              {isRunningTimer && (
                                <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 text-[10px] font-bold animate-pulse">
                                  Timer Running
                                </span>
                              )}
                            </div>

                            {acc.nicheTag && (
                              <div className="mt-1 flex items-center gap-1 text-indigo-400 text-xs font-medium">
                                <Tag className="w-3 h-3" />
                                <span>{acc.nicheTag}</span>
                              </div>
                            )}

                            {acc.notes && (
                              <p className="text-xs text-slate-400 mt-1 line-clamp-1">{acc.notes}</p>
                            )}
                          </div>

                          <div className="flex items-center gap-1.5 shrink-0 ml-3">
                            <button
                              onClick={() => {
                                setSimAccount(acc);
                                setCurrentView('ASSIST');
                              }}
                              className="px-2.5 py-1.5 rounded-lg bg-purple-600/20 text-purple-300 hover:bg-purple-600 hover:text-white transition text-xs font-semibold flex items-center gap-1"
                              title="Assist Engine"
                            >
                              <Bot className="w-3.5 h-3.5" />
                              <span>Assist</span>
                            </button>
                            <button
                              onClick={() => handleStartTimer(acc)}
                              className="px-2.5 py-1.5 rounded-lg bg-indigo-600/20 text-indigo-300 hover:bg-indigo-600 hover:text-white transition text-xs font-semibold flex items-center gap-1"
                              title="Start timer for account"
                            >
                              <Play className="w-3.5 h-3.5 fill-current" />
                              <span>Timer</span>
                            </button>
                            <button
                              onClick={() => handleOpenEditDialog(acc)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
                              title="Edit account"
                            >
                              <Edit2 className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleDeleteAccount(acc.id)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition"
                              title="Delete account"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>

                        <div
                          onClick={() => {
                            setSelectedAccountId(acc.id);
                            setCurrentView('DETAIL');
                          }}
                          className="mt-3 cursor-pointer pt-3 border-t border-slate-800/80"
                        >
                          {calc.status.type === 'NOT_STARTED' && (
                            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-lg bg-slate-800 text-slate-400 text-xs font-semibold">
                              <span className="w-2 h-2 rounded-full bg-slate-500" />
                              <span>Not started</span>
                            </div>
                          )}

                          {calc.status.type === 'WARMING_UP' && (
                            <div className="space-y-1.5">
                              <div className="flex items-center justify-between text-xs">
                                <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-lg bg-amber-500/20 text-amber-300 font-semibold">
                                  <span className="w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
                                  <span>Warming up - Day {calc.status.currentDay} of {calc.status.targetDays}</span>
                                </div>
                                <span className="text-slate-400 text-[11px]">
                                  {Math.round(calc.status.progress * 100)}% complete
                                </span>
                              </div>
                              <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                                <div
                                  className="h-full bg-gradient-to-r from-amber-500 to-amber-400 rounded-full transition-all"
                                  style={{ width: `${calc.status.progress * 100}%` }}
                                />
                              </div>
                            </div>
                          )}

                          {calc.status.type === 'WARMED_UP' && (
                            <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-lg bg-emerald-500/20 text-emerald-300 text-xs font-semibold">
                              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                              <span>Warmed up</span>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              );
            })()}
          </div>
        )}

        {/* VIEW: ACCOUNT DETAIL & HISTORY */}
        {currentView === 'DETAIL' && selectedCalc && (
          <div className="space-y-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setCurrentView('PLATFORM')}
                  className="p-2 bg-slate-900 border border-slate-800 rounded-xl text-slate-400 hover:text-white transition"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <div>
                  <h2 className="text-xl font-bold text-white flex items-center gap-2">
                    {selectedCalc.account.username}
                  </h2>
                  <p className="text-xs text-slate-400">
                    {selectedCalc.account.platform} • Target: {selectedCalc.account.targetDailyMinutes}m/day for {selectedCalc.account.targetDays} consecutive days
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => {
                    setSimAccount(selectedCalc.account);
                    setCurrentView('ASSIST');
                  }}
                  className="px-3 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-purple-600/20 transition flex items-center gap-1.5"
                >
                  <Bot className="w-4 h-4" />
                  <span>Assist Mode</span>
                </button>
                <button
                  onClick={() => handleStartTimer(selectedCalc.account)}
                  className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-emerald-600/20 transition flex items-center gap-1.5"
                >
                  <Play className="w-4 h-4 fill-current" />
                  <span>Start Timer</span>
                </button>
              </div>
            </div>

            {selectedCalc.hasMissedDay && (
              <div className="bg-amber-950/40 border border-amber-500/40 rounded-2xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-lg">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
                  <div>
                    <h4 className="text-sm font-semibold text-amber-300">Missed Warmup Day Detected</h4>
                    <p className="text-xs text-slate-300 mt-0.5 leading-relaxed">
                      You didn't reach the {selectedCalc.account.targetDailyMinutes} min target yesterday. Choose whether to forgive this gap or restart your consecutive streak.
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                  <button
                    onClick={handleResetStreak}
                    className="px-3 py-1.5 bg-slate-900 border border-amber-500/50 hover:bg-slate-800 text-amber-300 text-xs font-medium rounded-lg transition"
                  >
                    Reset Streak
                  </button>
                  <button
                    onClick={handleKeepStreak}
                    className="px-3 py-1.5 bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-semibold rounded-lg transition"
                  >
                    Keep Progress
                  </button>
                </div>
              </div>
            )}

            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-800">
                <div>
                  <span className="text-xs text-slate-400">Current Warmup Status</span>
                  <div className="mt-1">
                    {selectedCalc.status.type === 'NOT_STARTED' && (
                      <span className="inline-flex items-center gap-2 px-3 py-1 rounded-lg bg-slate-800 text-slate-300 text-sm font-semibold">
                        <span className="w-2.5 h-2.5 rounded-full bg-slate-500" />
                        Not started
                      </span>
                    )}
                    {selectedCalc.status.type === 'WARMING_UP' && (
                      <span className="inline-flex items-center gap-2 px-3 py-1 rounded-lg bg-amber-500/20 text-amber-300 text-sm font-semibold">
                        <span className="w-2.5 h-2.5 rounded-full bg-amber-400 animate-pulse" />
                        Warming up - Day {selectedCalc.status.currentDay} of {selectedCalc.status.targetDays}
                      </span>
                    )}
                    {selectedCalc.status.type === 'WARMED_UP' && (
                      <span className="inline-flex items-center gap-2 px-3 py-1 rounded-lg bg-emerald-500/20 text-emerald-300 text-sm font-semibold">
                        <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                        Warmed up
                      </span>
                    )}
                  </div>
                </div>

                <div className="text-right">
                  <span className="text-xs text-slate-400">Today's Progress</span>
                  <p className="text-sm font-bold text-white mt-1">
                    {selectedCalc.todayMinutes} / {selectedCalc.account.targetDailyMinutes} mins
                    {selectedCalc.todayMinutes >= selectedCalc.account.targetDailyMinutes && (
                      <span className="ml-1 text-emerald-400 font-semibold">(Met ✓)</span>
                    )}
                  </p>
                </div>
              </div>

              {selectedCalc.status.type === 'WARMING_UP' && (
                <div className="space-y-1.5">
                  <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-amber-500 to-amber-400 rounded-full"
                      style={{ width: `${selectedCalc.status.progress * 100}%` }}
                    />
                  </div>
                  <div className="flex justify-between text-[11px] text-slate-400">
                    <span>{selectedCalc.status.currentDay} qualifying days completed</span>
                    <span>{selectedCalc.status.targetDays - selectedCalc.status.currentDay} days remaining</span>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-3 gap-3 pt-2">
                <div className="bg-slate-800/40 border border-slate-700/50 rounded-xl p-3 text-center">
                  <Clock className="w-4 h-4 text-sky-400 mx-auto mb-1" />
                  <p className="text-lg font-bold text-white">{selectedCalc.totalMinutes}m</p>
                  <p className="text-[11px] text-slate-400">Total Minutes</p>
                </div>
                <div className="bg-slate-800/40 border border-slate-700/50 rounded-xl p-3 text-center">
                  <Heart className="w-4 h-4 text-rose-400 mx-auto mb-1" />
                  <p className="text-lg font-bold text-white">{selectedCalc.totalLikes}</p>
                  <p className="text-[11px] text-slate-400">Likes Done</p>
                </div>
                <div className="bg-slate-800/40 border border-slate-700/50 rounded-xl p-3 text-center">
                  <Bookmark className="w-4 h-4 text-amber-400 mx-auto mb-1" />
                  <p className="text-lg font-bold text-white">{selectedCalc.totalSaves}</p>
                  <p className="text-[11px] text-slate-400">Saves Done</p>
                </div>
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-white">Daily History (Aggregated)</h3>
                <span className="text-xs text-slate-500">Days count when minutes ≥ {selectedCalc.account.targetDailyMinutes}m</span>
              </div>

              {selectedCalc.dailyList.length === 0 ? (
                <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 text-center text-xs text-slate-500">
                  No sessions recorded for this account yet.
                </div>
              ) : (
                <div className="space-y-2">
                  {selectedCalc.dailyList.map(daily => (
                    <div
                      key={daily.dateString}
                      className="bg-slate-900 border border-slate-800 rounded-xl p-3.5 flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-7 h-7 rounded-lg flex items-center justify-center font-bold ${
                            daily.isTargetMet
                              ? 'bg-emerald-500/20 text-emerald-400'
                              : 'bg-amber-500/20 text-amber-400'
                          }`}
                        >
                          {daily.isTargetMet ? '✓' : '•'}
                        </div>
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-white text-sm">{daily.dateString}</span>
                            {daily.isTargetMet ? (
                              <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 text-[10px] font-bold">
                                Target Met
                              </span>
                            ) : (
                              <span className="px-2 py-0.5 rounded bg-amber-500/20 text-amber-400 text-[10px] font-bold">
                                Partial ({daily.totalMinutes}/{selectedCalc.account.targetDailyMinutes}m)
                              </span>
                            )}
                          </div>
                          <p className="text-slate-400 text-[11px] mt-0.5">
                            {daily.totalSessions} sessions • {daily.totalLikes} likes, {daily.totalSaves} saves
                          </p>
                        </div>
                      </div>

                      <div className="text-right">
                        <span className="text-base font-bold text-white">{daily.totalMinutes} min</span>
                        <p className="text-[10px] text-slate-500">
                          {daily.isTargetMet ? 'Counted to streak' : 'Needs more time'}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* VIEW: ROADMAP & ALL 3 STAGES */}
        {currentView === 'ROADMAP' && (
          <div className="space-y-6">
            <div className="flex items-center gap-3">
              <button
                onClick={() => setCurrentView('HOME')}
                className="p-2 bg-slate-900 border border-slate-800 rounded-xl text-slate-400 hover:text-white transition"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <div>
                <h2 className="text-xl font-bold text-white">Full 3-Stage Architecture Rollout</h2>
                <p className="text-xs text-slate-400">All three stages completed and ready to compile for Android</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-slate-900 border border-emerald-500/50 rounded-2xl p-5 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-emerald-400 uppercase tracking-wider">Stage 1</span>
                  <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 text-[10px] font-bold">
                    Completed ✓
                  </span>
                </div>
                <h3 className="font-bold text-base text-white">Account Tracker & Warmup Logic</h3>
                <ul className="text-xs text-slate-300 space-y-1.5 list-disc list-inside">
                  <li>Room DB Entities & DAOs for Accounts & Sessions</li>
                  <li>Target days (3, 5, 7; default 5) & minutes (30m)</li>
                  <li>Split session daily aggregation</li>
                  <li>Consecutive streak tracking</li>
                  <li>Missed day alert & keep/reset choice</li>
                  <li>CSV Export & Import</li>
                </ul>
              </div>

              <div className="bg-slate-900 border border-indigo-500/50 rounded-2xl p-5 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-indigo-400 uppercase tracking-wider">Stage 2</span>
                  <span className="px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 text-[10px] font-bold">
                    Completed ✓
                  </span>
                </div>
                <h3 className="font-bold text-base text-white">Manual Timer & Notifications</h3>
                <ul className="text-xs text-slate-300 space-y-1.5 list-disc list-inside">
                  <li>Floating overlay timer (SYSTEM_ALERT_WINDOW)</li>
                  <li>Real-time stopwatch over TikTok/IG/YouTube</li>
                  <li>Real-time +1 Like & +1 Save counters</li>
                  <li>WorkManager daily reminders for unmet targets</li>
                  <li>Foreground Service with persistent notification</li>
                  <li>Auto-save session upon tapping Stop</li>
                </ul>
              </div>

              <div className="bg-slate-900 border-2 border-purple-500/60 rounded-2xl p-5 space-y-3 shadow-lg shadow-purple-500/10">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-purple-400 uppercase tracking-wider">Stage 3</span>
                  <span className="px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 text-[10px] font-bold">
                    Completed ✓
                  </span>
                </div>
                <h3 className="font-bold text-base text-white">Assist Mode Engine</h3>
                <ul className="text-xs text-slate-300 space-y-1.5 list-disc list-inside">
                  <li>AccessibilityService dispatchGesture engine</li>
                  <li>Weighted reactions: Instant skip 25%, glance 20%, partial 25%, full 20%, rewatch 10%</li>
                  <li>Dynamic Bezier curve swipe & jitter</li>
                  <li>Heart button vs double-tap like rotation</li>
                  <li>Niche bias keyword detection & auto-stop on captcha</li>
                </ul>
              </div>
            </div>

            {/* Honest Technical Reality Section */}
            <div className="bg-slate-900 border border-amber-500/30 rounded-2xl p-6 space-y-4">
              <div className="flex items-center gap-3">
                <AlertTriangle className="w-6 h-6 text-amber-400" />
                <h3 className="text-base font-bold text-white">Honest Technical Analysis: Android & App Fragility</h3>
              </div>

              <div className="space-y-3 text-xs text-slate-300 leading-relaxed">
                <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-800">
                  <strong className="text-amber-400 block mb-1">1. Obfuscated UI Hierarchy & Fragile Selectors</strong>
                  TikTok and Instagram update their native client binaries weekly via Play Store and dynamic server-driven feature flags. View IDs (e.g. <code>like_icon</code>) change constantly. Assist mode relies on fallback heuristics (relative bounds, content descriptions like "Like", "Save", or double-tap coordinates) rather than brittle static IDs.
                </div>

                <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-800">
                  <strong className="text-amber-400 block mb-1">2. Hardware Sensor & Touch Telemetry Detection</strong>
                  TikTok and Meta SDKs inspect raw <code>MotionEvent</code> data: Android's <code>AccessibilityService.dispatchGesture</code> generates touch paths without real physical gyroscope, accelerometer, touch pressure variance, or finger contact area. High-frequency bot detection algorithms look for these missing sensor streams. That's why Stage 3 includes human jitter, variable velocity curves, and conservative daily caps.
                </div>

                <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-800">
                  <strong className="text-amber-400 block mb-1">3. Android OS Accessibility Permissions & Background Killing</strong>
                  OEMs aggressively kill long-running AccessibilityServices in the background. AccessibilityService must be paired with battery optimization exemption (<code>REQUEST_IGNORE_BATTERY_OPTIMIZATIONS</code>) and user re-enablement checks.
                </div>
              </div>
            </div>
          </div>
        )}

        {/* VIEW: GRADLE BUILD & INSTALL GUIDE */}
        {currentView === 'GRADLE' && (
          <div className="space-y-6">
            <div className="flex items-center gap-3">
              <button
                onClick={() => setCurrentView('HOME')}
                className="p-2 bg-slate-900 border border-slate-800 rounded-xl text-slate-400 hover:text-white transition"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <div>
                <h2 className="text-xl font-bold text-white">Android Gradle Build & Windows Phone Install</h2>
                <p className="text-xs text-slate-400">Step-by-step instructions for Windows terminal, Android Studio, and ADB</p>
              </div>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-5">
              <div>
                <h3 className="text-sm font-bold text-indigo-400 uppercase tracking-wider mb-2">Stage 3 Android Files Generated</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] font-mono text-slate-300 bg-slate-950 p-3 rounded-xl border border-slate-800">
                  <div>• service/WarmUpAccessibilityService.kt</div>
                  <div>• engine/HumanBehaviorEngine.kt</div>
                  <div>• data/model/AssistSettings.kt</div>
                  <div>• res/xml/accessibility_service_config.xml</div>
                  <div>• ui/screens/settings/AssistSettingsScreen.kt</div>
                  <div>• AndroidManifest.xml (BIND_ACCESSIBILITY_SERVICE)</div>
                </div>
              </div>

              <div className="space-y-3">
                <h3 className="text-sm font-bold text-white">How to Build on Windows:</h3>
                <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                  <p className="text-slate-400 mb-1 font-semibold text-xs">Run Gradle wrapper in PowerShell / Command Prompt:</p>
                  <pre className="text-indigo-300 font-mono text-[12px] bg-slate-900 p-2.5 rounded-lg overflow-x-auto">
{`# 1. Open Terminal or PowerShell in this folder
# 2. Run Gradle wrapper assembleDebug:
.\\gradlew.bat assembleDebug

# Output APK location:
# app\\build\\outputs\\apk\\debug\\app-debug.apk`}
                  </pre>
                </div>
              </div>

              <div className="space-y-3">
                <h3 className="text-sm font-bold text-white">How to Install on Your Android Phone:</h3>
                <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 space-y-2 text-xs text-slate-300">
                  <p className="font-semibold text-slate-200">1. Enable USB Debugging on your phone:</p>
                  <p className="text-slate-400">
                    Go to <strong>Settings → About phone</strong>, tap <strong>Build number 7 times</strong> to enable Developer options. Then turn on <strong>USB debugging</strong>.
                  </p>

                  <p className="font-semibold text-slate-200 pt-2">2. Connect via USB cable and install with ADB:</p>
                  <pre className="text-emerald-300 font-mono text-[12px] bg-slate-900 p-2.5 rounded-lg overflow-x-auto">
{`adb devices
adb install -r app\\build\\outputs\\apk\\debug\\app-debug.apk`}
                  </pre>

                  <p className="font-semibold text-slate-200 pt-2">3. Enable AccessibilityService on your phone:</p>
                  <p className="text-slate-400">
                    After opening the app on your phone, go to <strong>Settings → Accessibility → Installed Apps → WarmUp Manager</strong> and toggle it <strong>ON</strong>.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* FLOATING STOPWATCH CONTROLLER (STAGE 2) */}
      {activeTimerAccount && (
        <div className="fixed bottom-6 right-6 z-50 max-w-sm w-full transition-all">
          {isTimerMinimized ? (
            <div
              onClick={() => setIsTimerMinimized(false)}
              className="ml-auto bg-slate-900/95 border-2 border-indigo-500 rounded-full px-4 py-2.5 shadow-2xl backdrop-blur flex items-center gap-3 cursor-pointer hover:scale-105 transition"
            >
              <div className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
              <span className="font-bold text-xs text-white">
                {activeTimerAccount.username} • {formattedStopwatch}
              </span>
              <Maximize2 className="w-3.5 h-3.5 text-indigo-400" />
            </div>
          ) : (
            <div className="bg-slate-900/95 border-2 border-indigo-500/80 rounded-2xl p-4 shadow-2xl backdrop-blur space-y-3">
              <div className="flex items-center justify-between pb-2 border-b border-slate-800">
                <div className="flex items-center gap-2">
                  <div
                    className={`w-2 h-2 rounded-full ${
                      activeTimerAccount.platform === 'TIKTOK'
                        ? 'bg-rose-500'
                        : activeTimerAccount.platform === 'INSTAGRAM'
                        ? 'bg-pink-500'
                        : 'bg-red-500'
                    }`}
                  />
                  <span className="font-bold text-sm text-white">{activeTimerAccount.username}</span>
                  <span className="text-[10px] text-slate-400">({activeTimerAccount.platform})</span>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setIsTurboSpeed(!isTurboSpeed)}
                    className={`px-1.5 py-0.5 rounded text-[10px] font-bold flex items-center gap-1 ${
                      isTurboSpeed
                        ? 'bg-amber-500 text-slate-950 animate-pulse'
                        : 'bg-slate-800 text-slate-400 hover:text-white'
                    }`}
                    title="Turbo test speed (1 sec = 1 min)"
                  >
                    <FastForward className="w-3 h-3" />
                    <span>{isTurboSpeed ? 'Turbo 60x' : '1x Speed'}</span>
                  </button>
                  <button
                    onClick={() => setIsTimerMinimized(true)}
                    className="p-1 text-slate-400 hover:text-white transition"
                    title="Minimize to floating pill"
                  >
                    <Minimize2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-between">
                <div>
                  <div className="text-3xl font-black font-mono tracking-wider text-white">
                    {formattedStopwatch}
                  </div>
                  <p className="text-[11px] text-slate-400">
                    Daily target: {timerTargetMins}m ({Math.round(timerProgress * 100)}% reached)
                  </p>
                </div>

                <div className="relative w-12 h-12 flex items-center justify-center">
                  <svg className="w-full h-full -rotate-90" viewBox="0 0 36 36">
                    <path
                      className="text-slate-800"
                      strokeWidth="3.5"
                      stroke="currentColor"
                      fill="none"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    />
                    <path
                      className={timerProgress >= 1 ? 'text-emerald-400' : 'text-indigo-500'}
                      strokeDasharray={`${timerProgress * 100}, 100`}
                      strokeWidth="3.5"
                      strokeLinecap="round"
                      stroke="currentColor"
                      fill="none"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    />
                  </svg>
                  <span className="absolute text-[10px] font-bold text-white">
                    {Math.round(timerProgress * 100)}%
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 pt-1">
                <button
                  onClick={() => setTimerLikes(l => l + 1)}
                  className="px-3 py-2 bg-rose-950/40 hover:bg-rose-950/60 border border-rose-500/30 text-rose-300 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition active:scale-95"
                >
                  <Heart className="w-3.5 h-3.5 fill-rose-500 text-rose-500" />
                  <span>+1 Like ({timerLikes})</span>
                </button>
                <button
                  onClick={() => setTimerSaves(s => s + 1)}
                  className="px-3 py-2 bg-amber-950/40 hover:bg-amber-950/60 border border-amber-500/30 text-amber-300 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition active:scale-95"
                >
                  <Bookmark className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                  <span>+1 Save ({timerSaves})</span>
                </button>
              </div>

              <div className="flex items-center gap-2 pt-1">
                <button
                  onClick={handleCancelTimer}
                  className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-400 rounded-xl text-xs font-semibold transition"
                >
                  Discard
                </button>
                <button
                  onClick={handleStopAndSaveTimer}
                  className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold shadow-lg shadow-emerald-600/30 transition flex items-center justify-center gap-1.5"
                >
                  <Square className="w-3.5 h-3.5 fill-current" />
                  <span>Stop & Save Session</span>
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* MODAL: WORKMANAGER NOTIFICATION SIMULATOR */}
      {isNotificationDialogOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-amber-500/20 rounded-xl text-amber-400">
                  <Bell className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">Daily Reminder (WorkManager)</h3>
                  <p className="text-xs text-slate-400">Periodic reminder notification scheduled via WorkManager</p>
                </div>
              </div>
            </div>

            <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2">
              <div className="flex items-center justify-between text-xs text-slate-400 pb-2 border-b border-slate-800">
                <span className="font-semibold text-slate-200">System Notification Channel</span>
                <span className="text-emerald-400 font-mono">IMPORTANCE_DEFAULT</span>
              </div>
              <p className="text-xs font-bold text-white pt-1">WarmUp Manager: Time to Warm Up</p>
              <div className="text-xs text-slate-300 space-y-1">
                {accountsNeedingTimeToday.length === 0 ? (
                  <p className="text-slate-400">All accounts are up to date! No reminders triggered.</p>
                ) : (
                  <>
                    <p className="text-amber-300 font-medium">
                      {accountsNeedingTimeToday.length} {accountsNeedingTimeToday.length === 1 ? 'account needs' : 'accounts need'} warmup today to preserve streak:
                    </p>
                    <ul className="list-disc list-inside space-y-0.5 text-[11px] text-slate-400">
                      {accountsNeedingTimeToday.map(c => (
                        <li key={c.account.id}>
                          {c.account.username} ({c.account.targetDailyMinutes - c.todayMinutes}m remaining)
                        </li>
                      ))}
                    </ul>
                  </>
                )}
              </div>
            </div>

            <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-800 text-[11px] text-slate-400">
              In Android, <code>DailyReminderWorker</code> executes periodically in the background via <code>WarmUpWorkManager.scheduleDailyReminders()</code>, verifying each account's qualifying daily status without draining battery.
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setIsNotificationDialogOpen(false)}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold transition"
              >
                Close Preview
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: ADD / EDIT ACCOUNT */}
      {isAddAccountOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <h3 className="text-base font-bold text-white">
              {accountToEdit ? 'Edit Account' : `Add ${selectedPlatform} Account`}
            </h3>

            <form onSubmit={handleSaveAccount} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Username / Handle</label>
                <input
                  type="text"
                  required
                  placeholder="@your_account"
                  value={formUsername}
                  onChange={e => setFormUsername(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Niche Tag</label>
                <input
                  type="text"
                  placeholder="e.g. Tech, Fitness, Gaming, Finance"
                  value={formNiche}
                  onChange={e => setFormNiche(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Notes</label>
                <input
                  type="text"
                  placeholder="Proxy, device slot, SIM, creation date"
                  value={formNotes}
                  onChange={e => setFormNotes(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-medium text-slate-400">Warmup Target Days</label>
                <div className="grid grid-cols-3 gap-2">
                  {[3, 5, 7].map(days => (
                    <button
                      type="button"
                      key={days}
                      onClick={() => setFormTargetDays(days)}
                      className={`py-2 rounded-xl text-xs font-semibold border transition ${
                        formTargetDays === days
                          ? 'bg-indigo-600 border-indigo-500 text-white'
                          : 'bg-slate-950 border-slate-800 text-slate-400 hover:bg-slate-800'
                      }`}
                    >
                      {days} Days {days === 5 && '★'}
                    </button>
                  ))}
                </div>
                <p className="text-[11px] text-slate-500">5 days recommended to avoid algorithmic shadowbans.</p>
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-medium text-slate-400">Target Daily Minutes</label>
                <div className="grid grid-cols-3 gap-2">
                  {[15, 30, 45].map(mins => (
                    <button
                      type="button"
                      key={mins}
                      onClick={() => setFormTargetMins(mins)}
                      className={`py-2 rounded-xl text-xs font-semibold border transition ${
                        formTargetMins === mins
                          ? 'bg-indigo-600 border-indigo-500 text-white'
                          : 'bg-slate-950 border-slate-800 text-slate-400 hover:bg-slate-800'
                      }`}
                    >
                      {mins} min/day
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsAddAccountOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-indigo-600/20 transition"
                >
                  {accountToEdit ? 'Save Changes' : 'Add Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: MANUAL LOG WARMUP SESSION */}
      {isLogSessionOpen && selectedCalc && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-4">
            <div>
              <h3 className="text-base font-bold text-white">Log Warmup Session (Manual)</h3>
              <p className="text-xs text-slate-400">Manually enter a past session for {selectedCalc.account.username}</p>
            </div>

            <form onSubmit={handleAddManualSession} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Date</label>
                <input
                  type="date"
                  required
                  value={sessDate}
                  onChange={e => setSessDate(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-medium text-slate-400">Duration (Minutes)</label>
                <div className="grid grid-cols-4 gap-2">
                  {[10, 15, 20, 30].map(mins => (
                    <button
                      type="button"
                      key={mins}
                      onClick={() => setSessDuration(mins)}
                      className={`py-2 rounded-xl text-xs font-semibold border transition ${
                        sessDuration === mins
                          ? 'bg-indigo-600 border-indigo-500 text-white'
                          : 'bg-slate-950 border-slate-800 text-slate-400 hover:bg-slate-800'
                      }`}
                    >
                      {mins}m
                    </button>
                  ))}
                </div>
                <input
                  type="number"
                  min="1"
                  max="180"
                  value={sessDuration}
                  onChange={e => setSessDuration(parseInt(e.target.value) || 1)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500 mt-1"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Likes Done</label>
                  <input
                    type="number"
                    min="0"
                    value={sessLikes}
                    onChange={e => setSessLikes(parseInt(e.target.value) || 0)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Saves Done</label>
                  <input
                    type="number"
                    min="0"
                    value={sessSaves}
                    onChange={e => setSessSaves(parseInt(e.target.value) || 0)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsLogSessionOpen(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-indigo-600/20 transition"
                >
                  Record Session
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
