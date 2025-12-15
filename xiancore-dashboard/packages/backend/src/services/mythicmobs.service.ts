import * as yaml from 'js-yaml';
import * as fs from 'fs';
import * as path from 'path';

/**
 * 技能信息
 */
export interface MythicSkillInfo {
  raw: string;           // 原始技能字符串
  mechanic?: string;     // 技能机制
  trigger?: string;      // 触发器
  conditions?: string[]; // 条件
}

/**
 * 掉落物信息
 */
export interface MythicDropInfo {
  raw: string;           // 原始掉落字符串
  item?: string;         // 物品
  amount?: string;       // 数量
  chance?: number;       // 概率
}

/**
 * 装备信息
 */
export interface MythicEquipmentInfo {
  slot: string;          // 装备槽位
  item: string;          // 物品
}

/**
 * MythicMob 怪物基础信息（用于列表展示）
 */
export interface MythicMobInfo {
  id: string;
  displayName: string;
  type: string;
  health: number;
  damage: number;
  armor: number;
  movementSpeed?: number;
  knockbackResistance?: number;
  skills?: string[];
  options?: Record<string, any>;
  fileName: string;
}

/**
 * MythicMob 怪物详细信息（包含完整配置）
 */
export interface MythicMobDetailInfo extends MythicMobInfo {
  // 原始配置
  rawConfig: Record<string, any>;

  // 解析后的技能
  parsedSkills: MythicSkillInfo[];

  // 掉落表
  drops: MythicDropInfo[];
  dropsTable?: string;  // 引用的掉落表名称

  // 装备
  equipment: MythicEquipmentInfo[];

  // AI 行为
  aiGoals?: string[];
  aiTargets?: string[];

  // 伪装
  disguise?: string;

  // 等级配置
  levelModifiers?: Record<string, any>;

  // 派系
  faction?: string;

  // Boss 血条
  bossBar?: {
    enabled: boolean;
    title?: string;
    color?: string;
    style?: string;
  };

  // 听力/视觉范围
  hearingRange?: number;
  followRange?: number;

  // 其他选项
  preventOtherDrops?: boolean;
  preventRandomEquipment?: boolean;
  preventLeashing?: boolean;
  preventSunburn?: boolean;
}

/**
 * MythicMobs 配置读取服务
 * 直接读取 MythicMobs 插件的 YAML 配置文件
 */
export class MythicMobsService {
  private mobsPath: string;
  private cache: Map<string, MythicMobInfo> = new Map();
  private lastCacheTime: number = 0;
  private cacheExpireMs: number = 60000; // 缓存 60 秒

  constructor() {
    // 从环境变量读取 MythicMobs 配置目录路径
    this.mobsPath = process.env.MYTHICMOBS_MOBS_PATH || '';
  }

  /**
   * 检查配置路径是否有效
   */
  isConfigured(): boolean {
    if (!this.mobsPath) {
      return false;
    }
    try {
      return fs.existsSync(this.mobsPath) && fs.statSync(this.mobsPath).isDirectory();
    } catch {
      return false;
    }
  }

  /**
   * 获取配置路径
   */
  getConfigPath(): string {
    return this.mobsPath;
  }

  /**
   * 设置配置路径（用于动态更新）
   */
  setConfigPath(newPath: string): void {
    this.mobsPath = newPath;
    this.clearCache();
  }

  /**
   * 清除缓存
   */
  clearCache(): void {
    this.cache.clear();
    this.lastCacheTime = 0;
  }

  /**
   * 检查缓存是否过期
   */
  private isCacheExpired(): boolean {
    return Date.now() - this.lastCacheTime > this.cacheExpireMs;
  }

  /**
   * 获取所有 MythicMob 怪物列表
   */
  async getAllMobs(): Promise<MythicMobInfo[]> {
    if (!this.isConfigured()) {
      console.warn('MythicMobs 配置路径未设置或无效:', this.mobsPath);
      return [];
    }

    // 检查缓存
    if (!this.isCacheExpired() && this.cache.size > 0) {
      return Array.from(this.cache.values());
    }

    const mobs: MythicMobInfo[] = [];

    try {
      const files = await fs.promises.readdir(this.mobsPath);

      for (const file of files) {
        if (!file.endsWith('.yml') && !file.endsWith('.yaml')) {
          continue;
        }

        try {
          const filePath = path.join(this.mobsPath, file);
          const content = await fs.promises.readFile(filePath, 'utf8');
          const parsed = yaml.load(content) as Record<string, any>;

          if (!parsed || typeof parsed !== 'object') {
            continue;
          }

          // 遍历文件中定义的所有怪物
          for (const [id, config] of Object.entries(parsed)) {
            if (!config || typeof config !== 'object') {
              continue;
            }

            const mobInfo = this.parseMobConfig(id, config, file);
            if (mobInfo) {
              mobs.push(mobInfo);
              this.cache.set(id, mobInfo);
            }
          }
        } catch (fileError) {
          console.error(`解析文件 ${file} 失败:`, fileError);
        }
      }

      this.lastCacheTime = Date.now();
    } catch (error) {
      console.error('读取 MythicMobs 配置目录失败:', error);
    }

    return mobs;
  }

  /**
   * 解析单个怪物配置
   */
  private parseMobConfig(id: string, config: any, fileName: string): MythicMobInfo | null {
    try {
      // 清理显示名称中的颜色代码
      let displayName = config.Display || config.DisplayName || id;
      displayName = this.stripColorCodes(displayName);

      const mobInfo: MythicMobInfo = {
        id,
        displayName,
        type: config.Type || 'ZOMBIE',
        health: this.parseNumber(config.Health, 20),
        damage: this.parseNumber(config.Damage, 5),
        armor: this.parseNumber(config.Armor, 0),
        fileName,
      };

      // 解析 Options
      if (config.Options && typeof config.Options === 'object') {
        mobInfo.options = config.Options;
        mobInfo.movementSpeed = this.parseNumber(config.Options.MovementSpeed, 0.2);
        mobInfo.knockbackResistance = this.parseNumber(config.Options.KnockbackResistance, 0);
      }

      // 解析技能列表
      if (config.Skills && Array.isArray(config.Skills)) {
        mobInfo.skills = config.Skills.map((s: any) => String(s));
      }

      return mobInfo;
    } catch (error) {
      console.error(`解析怪物配置 ${id} 失败:`, error);
      return null;
    }
  }

  /**
   * 解析数字值（支持范围格式如 "100-200"）
   */
  private parseNumber(value: any, defaultValue: number): number {
    if (value === undefined || value === null) {
      return defaultValue;
    }

    if (typeof value === 'number') {
      return value;
    }

    if (typeof value === 'string') {
      // 处理范围格式 "100-200"，取中间值
      if (value.includes('-')) {
        const parts = value.split('-');
        const min = parseFloat(parts[0]);
        const max = parseFloat(parts[1]);
        if (!isNaN(min) && !isNaN(max)) {
          return (min + max) / 2;
        }
      }
      const num = parseFloat(value);
      return isNaN(num) ? defaultValue : num;
    }

    return defaultValue;
  }

  /**
   * 移除 Minecraft 颜色代码
   */
  private stripColorCodes(text: string): string {
    if (!text) return '';
    // 移除 &x 和 §x 格式的颜色代码
    return text.replace(/[&§][0-9a-fk-or]/gi, '').trim();
  }

  /**
   * 根据 ID 获取单个怪物信息
   */
  async getMobById(id: string): Promise<MythicMobInfo | null> {
    // 先检查缓存
    if (!this.isCacheExpired() && this.cache.has(id)) {
      return this.cache.get(id) || null;
    }

    // 重新加载所有怪物
    await this.getAllMobs();
    return this.cache.get(id) || null;
  }

  /**
   * 检查怪物 ID 是否存在
   */
  async hasMob(id: string): Promise<boolean> {
    const mob = await this.getMobById(id);
    return mob !== null;
  }

  /**
   * 获取所有怪物 ID 列表（用于下拉选择）
   */
  async getMobIds(): Promise<string[]> {
    const mobs = await this.getAllMobs();
    return mobs.map(m => m.id);
  }

  /**
   * 按类型筛选怪物
   */
  async getMobsByType(type: string): Promise<MythicMobInfo[]> {
    const mobs = await this.getAllMobs();
    return mobs.filter(m => m.type.toUpperCase() === type.toUpperCase());
  }

  /**
   * 搜索怪物（按 ID 或显示名称）
   */
  async searchMobs(keyword: string): Promise<MythicMobInfo[]> {
    const mobs = await this.getAllMobs();
    const lowerKeyword = keyword.toLowerCase();
    return mobs.filter(m =>
      m.id.toLowerCase().includes(lowerKeyword) ||
      m.displayName.toLowerCase().includes(lowerKeyword)
    );
  }

  /**
   * 获取服务状态
   */
  getStatus(): { configured: boolean; path: string; cacheSize: number; cacheAge: number } {
    return {
      configured: this.isConfigured(),
      path: this.mobsPath,
      cacheSize: this.cache.size,
      cacheAge: this.lastCacheTime ? Math.floor((Date.now() - this.lastCacheTime) / 1000) : -1,
    };
  }

  /**
   * 获取怪物详细信息（包含完整配置）
   */
  async getMobDetailById(id: string): Promise<MythicMobDetailInfo | null> {
    if (!this.isConfigured()) {
      return null;
    }

    try {
      // 首先获取基础信息
      const basicInfo = await this.getMobById(id);
      if (!basicInfo) {
        return null;
      }

      // 读取原始配置文件
      const filePath = path.join(this.mobsPath, basicInfo.fileName);
      const content = await fs.promises.readFile(filePath, 'utf8');
      const parsed = yaml.load(content) as Record<string, any>;

      if (!parsed || !parsed[id]) {
        return null;
      }

      const config = parsed[id];
      return this.parseDetailedMobConfig(id, config, basicInfo);
    } catch (error) {
      console.error(`获取怪物详情 ${id} 失败:`, error);
      return null;
    }
  }

  /**
   * 解析详细怪物配置
   */
  private parseDetailedMobConfig(id: string, config: any, basicInfo: MythicMobInfo): MythicMobDetailInfo {
    const detail: MythicMobDetailInfo = {
      ...basicInfo,
      rawConfig: config,
      parsedSkills: [],
      drops: [],
      equipment: [],
    };

    // 解析技能
    if (config.Skills && Array.isArray(config.Skills)) {
      detail.parsedSkills = config.Skills.map((skill: any) => this.parseSkill(skill));
    }

    // 解析掉落表
    if (config.Drops && Array.isArray(config.Drops)) {
      detail.drops = config.Drops.map((drop: any) => this.parseDrop(drop));
    }
    if (config.DropsTable) {
      detail.dropsTable = String(config.DropsTable);
    }

    // 解析装备
    if (config.Equipment && Array.isArray(config.Equipment)) {
      detail.equipment = this.parseEquipment(config.Equipment);
    }

    // 解析 AI 行为
    if (config.AIGoalSelectors && Array.isArray(config.AIGoalSelectors)) {
      detail.aiGoals = config.AIGoalSelectors.map((g: any) => String(g));
    }
    if (config.AITargetSelectors && Array.isArray(config.AITargetSelectors)) {
      detail.aiTargets = config.AITargetSelectors.map((t: any) => String(t));
    }

    // 解析伪装
    if (config.Disguise) {
      detail.disguise = String(config.Disguise);
    }

    // 解析等级修正
    if (config.LevelModifiers) {
      detail.levelModifiers = config.LevelModifiers;
    }

    // 解析派系
    if (config.Faction) {
      detail.faction = String(config.Faction);
    }

    // 解析 Boss 血条
    if (config.BossBar) {
      if (typeof config.BossBar === 'object') {
        detail.bossBar = {
          enabled: config.BossBar.Enabled !== false,
          title: config.BossBar.Title,
          color: config.BossBar.Color,
          style: config.BossBar.Style,
        };
      } else if (config.BossBar === true) {
        detail.bossBar = { enabled: true };
      }
    }

    // 解析 Options 中的更多配置
    if (config.Options) {
      const opts = config.Options;
      detail.hearingRange = this.parseNumber(opts.HearingRange, undefined as any);
      detail.followRange = this.parseNumber(opts.FollowRange, undefined as any);
      detail.preventOtherDrops = opts.PreventOtherDrops;
      detail.preventRandomEquipment = opts.PreventRandomEquipment;
      detail.preventLeashing = opts.PreventLeashing;
      detail.preventSunburn = opts.PreventSunburn;
    }

    return detail;
  }

  /**
   * 解析单个技能
   */
  private parseSkill(skill: any): MythicSkillInfo {
    const raw = String(skill);
    const result: MythicSkillInfo = { raw };

    // 尝试解析技能格式: - mechanic{params} @trigger ~conditions
    const mechanicMatch = raw.match(/^-?\s*(\w+)\s*\{?/);
    if (mechanicMatch) {
      result.mechanic = mechanicMatch[1];
    }

    const triggerMatch = raw.match(/@(\w+)/);
    if (triggerMatch) {
      result.trigger = triggerMatch[1];
    }

    const conditionMatches = raw.match(/~(\w+)/g);
    if (conditionMatches) {
      result.conditions = conditionMatches.map(c => c.substring(1));
    }

    return result;
  }

  /**
   * 解析单个掉落物
   */
  private parseDrop(drop: any): MythicDropInfo {
    const raw = String(drop);
    const result: MythicDropInfo = { raw };

    // 尝试解析格式: item amount chance
    // 例如: - diamond 1-3 0.5
    // 或: - mythicitem{item=xxx} 1 1.0
    const parts = raw.trim().replace(/^-\s*/, '').split(/\s+/);
    if (parts.length >= 1) {
      result.item = parts[0];
    }
    if (parts.length >= 2) {
      result.amount = parts[1];
    }
    if (parts.length >= 3) {
      const chance = parseFloat(parts[2]);
      if (!isNaN(chance)) {
        result.chance = chance;
      }
    }

    return result;
  }

  /**
   * 解析装备列表
   */
  private parseEquipment(equipment: any[]): MythicEquipmentInfo[] {
    const slots = ['MAINHAND', 'OFFHAND', 'HEAD', 'CHEST', 'LEGS', 'FEET'];
    const result: MythicEquipmentInfo[] = [];

    equipment.forEach((item, index) => {
      if (item) {
        const slot = slots[index] || `SLOT_${index}`;
        result.push({
          slot,
          item: String(item),
        });
      }
    });

    return result;
  }

  /**
   * 获取所有怪物的详细信息
   */
  async getAllMobsDetailed(): Promise<MythicMobDetailInfo[]> {
    const basicMobs = await this.getAllMobs();
    const detailedMobs: MythicMobDetailInfo[] = [];

    for (const mob of basicMobs) {
      const detail = await this.getMobDetailById(mob.id);
      if (detail) {
        detailedMobs.push(detail);
      }
    }

    return detailedMobs;
  }

  /**
   * 获取怪物类型统计
   */
  async getMobTypeStats(): Promise<Record<string, number>> {
    const mobs = await this.getAllMobs();
    const stats: Record<string, number> = {};

    for (const mob of mobs) {
      const type = mob.type.toUpperCase();
      stats[type] = (stats[type] || 0) + 1;
    }

    return stats;
  }
}

export default new MythicMobsService();
