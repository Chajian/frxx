import * as yaml from 'js-yaml';
import * as fs from 'fs';
import * as path from 'path';

/**
 * 技能参数
 */
export interface MythicSkillParam {
  key: string;
  value: string;
}

/**
 * 条件详细信息
 */
export interface MythicConditionInfo {
  raw: string;           // 原始条件字符串
  type: string;          // 条件类型
  params: MythicSkillParam[];  // 条件参数
  negated: boolean;      // 是否取反
}

/**
 * 目标选择器详细信息
 */
export interface MythicTargeterInfo {
  raw: string;           // 原始目标选择器字符串
  type: string;          // 目标选择器类型
  params: MythicSkillParam[];  // 目标选择器参数
}

/**
 * 技能信息 (增强版)
 */
export interface MythicSkillInfo {
  raw: string;           // 原始技能字符串
  mechanic?: string;     // 技能机制
  trigger?: string;      // 触发器
  triggerHealth?: number;  // 触发器血量阈值 (如 @onDamaged 0.5)
  conditions?: string[]; // 条件 (简化)
  parsedConditions?: MythicConditionInfo[];  // 详细解析的条件
  targetSelector?: string;  // 目标选择器 (简化)
  parsedTargeter?: MythicTargeterInfo;       // 详细解析的目标选择器
  params?: MythicSkillParam[];  // 技能参数
  chance?: number;       // 触发概率
  cooldown?: number;     // 冷却时间
  healthModifier?: string;  // 血量修正
}

/**
 * 技能组信息
 */
export interface MythicSkillGroupInfo {
  id: string;
  skills: MythicSkillInfo[];
  cooldown?: number;
  conditions?: MythicConditionInfo[];
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
 * 掉落表详细信息
 */
export interface MythicDropTableInfo {
  id: string;
  drops: MythicDropInfo[];
  totalWeight?: number;
  conditions?: MythicConditionInfo[];
}

/**
 * 模板继承信息
 */
export interface MythicTemplateInfo {
  id: string;
  parent?: string;        // 父模板 ID
  children: string[];     // 子模板 IDs
  depth: number;          // 继承深度
}

/**
 * MythicMobs 物品附魔信息
 */
export interface MythicEnchantmentInfo {
  enchantment: string;    // 附魔类型
  level: number;          // 附魔等级
}

/**
 * MythicMobs 物品属性修饰符
 */
export interface MythicAttributeInfo {
  attribute: string;      // 属性类型
  amount: number;         // 数值
  operation?: string;     // 操作类型
  slot?: string;          // 装备槽位
}

/**
 * MythicMobs 物品基础信息
 */
export interface MythicItemInfo {
  id: string;
  displayName: string;
  material: string;
  amount?: number;
  customModelData?: number;
  lore?: string[];
  enchantments?: MythicEnchantmentInfo[];
  attributes?: MythicAttributeInfo[];
  unbreakable?: boolean;
  hideFlags?: string[];
  color?: string;           // 皮革染色
  potionEffects?: string[];
  skullTexture?: string;    // 头颅材质
  nbt?: Record<string, any>;
  options?: Record<string, any>;
  fileName: string;
}

/**
 * MythicMobs 物品详细信息
 */
export interface MythicItemDetailInfo extends MythicItemInfo {
  rawConfig: Record<string, any>;
}

/**
 * MythicMob 怪物详细信息（包含完整配置）
 */
export interface MythicMobDetailInfo extends MythicMobInfo {
  // 原始配置
  rawConfig: Record<string, any>;

  // 解析后的技能
  parsedSkills: MythicSkillInfo[];

  // 技能组
  skillGroups?: MythicSkillGroupInfo[];

  // 掉落表
  drops: MythicDropInfo[];
  dropsTable?: string;  // 引用的掉落表名称
  expandedDropsTable?: MythicDropTableInfo;  // 展开的掉落表

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

  // 模板继承
  template?: string;       // 使用的模板
  templateInfo?: MythicTemplateInfo;  // 模板继承信息
}

/**
 * MythicMobs 配置读取服务
 * 直接读取 MythicMobs 插件的 YAML 配置文件
 */
export class MythicMobsService {
  private mobsPath: string;
  private cache: Map<string, MythicMobInfo> = new Map();
  private dropTableCache: Map<string, MythicDropTableInfo> = new Map();
  private skillGroupCache: Map<string, MythicSkillGroupInfo> = new Map();
  private templateCache: Map<string, MythicTemplateInfo> = new Map();
  private itemCache: Map<string, MythicItemInfo> = new Map();
  private lastCacheTime: number = 0;
  private cacheExpireMs: number = 60000; // 缓存 60 秒

  constructor() {
    // 从环境变量读取 MythicMobs 配置目录路径
    this.mobsPath = process.env.MYTHICMOBS_MOBS_PATH || '';
  }

  /**
   * 获取 MythicMobs 基础目录 (Mobs 目录的父目录)
   */
  private getBasePath(): string {
    return path.dirname(this.mobsPath);
  }

  /**
   * 获取 DropTables 目录路径
   */
  private getDropTablesPath(): string {
    return path.join(this.getBasePath(), 'DropTables');
  }

  /**
   * 获取 Skills 目录路径
   */
  private getSkillsPath(): string {
    return path.join(this.getBasePath(), 'Skills');
  }

  /**
   * 获取 Items 目录路径
   */
  private getItemsPath(): string {
    return path.join(this.getBasePath(), 'Items');
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
    this.dropTableCache.clear();
    this.skillGroupCache.clear();
    this.templateCache.clear();
    this.itemCache.clear();
    this.lastCacheTime = 0;
  }

  /**
   * 检查缓存是否过期
   */
  private isCacheExpired(): boolean {
    return Date.now() - this.lastCacheTime > this.cacheExpireMs;
  }

  /**
   * 递归读取目录下所有 yml 文件
   */
  private async getAllYmlFiles(dir: string, fileList: string[] = []): Promise<string[]> {
    try {
      const entries = await fs.promises.readdir(dir, { withFileTypes: true });

      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);

        if (entry.isDirectory()) {
          // 递归读取子目录
          await this.getAllYmlFiles(fullPath, fileList);
        } else if (entry.isFile() && (entry.name.endsWith('.yml') || entry.name.endsWith('.yaml'))) {
          fileList.push(fullPath);
        }
      }
    } catch (error) {
      console.error(`读取目录 ${dir} 失败:`, error);
    }

    return fileList;
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
      // 递归获取所有 yml 文件
      const files = await this.getAllYmlFiles(this.mobsPath);

      for (const filePath of files) {
        try {
          const content = await fs.promises.readFile(filePath, 'utf8');
          const parsed = yaml.load(content) as Record<string, any>;

          if (!parsed || typeof parsed !== 'object') {
            continue;
          }

          // 获取相对文件名用于显示
          const fileName = path.relative(this.mobsPath, filePath);

          // 遍历文件中定义的所有怪物
          for (const [id, config] of Object.entries(parsed)) {
            if (!config || typeof config !== 'object') {
              continue;
            }

            const mobInfo = this.parseMobConfig(id, config, fileName);
            if (mobInfo) {
              mobs.push(mobInfo);
              this.cache.set(id, mobInfo);
            }
          }
        } catch (fileError) {
          console.error(`解析文件 ${filePath} 失败:`, fileError);
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
   * 解析单个技能 (增强版)
   */
  private parseSkill(skill: any): MythicSkillInfo {
    const raw = String(skill);
    const result: MythicSkillInfo = { raw };

    // 尝试解析技能格式: - mechanic{params} @trigger ~conditions ?targeter
    // 例如: - damage{amount=10;element=fire} @onAttack ~hasAura{aura=burn} ?target{range=10}

    // 1. 解析技能机制和参数
    const mechanicMatch = raw.match(/^-?\s*(\w+)(?:\{([^}]*)\})?/);
    if (mechanicMatch) {
      result.mechanic = mechanicMatch[1];

      // 解析参数
      if (mechanicMatch[2]) {
        result.params = this.parseParams(mechanicMatch[2]);
      }
    }

    // 2. 解析触发器和触发器参数
    const triggerMatch = raw.match(/@(\w+)(?:\{([^}]*)\})?(?:\s+([\d.]+))?/);
    if (triggerMatch) {
      result.trigger = triggerMatch[1];

      // 解析触发器血量阈值 (如 @onDamaged 0.5)
      if (triggerMatch[3]) {
        const healthVal = parseFloat(triggerMatch[3]);
        if (!isNaN(healthVal)) {
          result.triggerHealth = healthVal;
        }
      }
    }

    // 3. 解析条件 (简化和详细)
    const conditionMatches = raw.match(/~!?(\w+)(?:\{([^}]*)\})?/g);
    if (conditionMatches) {
      result.conditions = [];
      result.parsedConditions = [];

      for (const condMatch of conditionMatches) {
        const parsed = this.parseCondition(condMatch);
        result.conditions.push(parsed.type);
        result.parsedConditions.push(parsed);
      }
    }

    // 4. 解析目标选择器
    const targeterMatch = raw.match(/\?(\w+)(?:\{([^}]*)\})?/);
    if (targeterMatch) {
      result.targetSelector = targeterMatch[1];
      result.parsedTargeter = {
        raw: targeterMatch[0],
        type: targeterMatch[1],
        params: targeterMatch[2] ? this.parseParams(targeterMatch[2]) : [],
      };
    }

    // 5. 解析触发概率 (chance=0.5)
    const chanceMatch = raw.match(/chance[=:]?([\d.]+)/i);
    if (chanceMatch) {
      result.chance = parseFloat(chanceMatch[1]);
    }

    // 6. 解析冷却时间 (cooldown=5)
    const cooldownMatch = raw.match(/cooldown[=:]?([\d.]+)/i);
    if (cooldownMatch) {
      result.cooldown = parseFloat(cooldownMatch[1]);
    }

    // 7. 解析血量修正 (=10%-50%)
    const healthModMatch = raw.match(/=([\d.]+%-?[\d.]*%?)/);
    if (healthModMatch) {
      result.healthModifier = healthModMatch[1];
    }

    return result;
  }

  /**
   * 解析参数字符串 (key=value;key2=value2)
   */
  private parseParams(paramsStr: string): MythicSkillParam[] {
    const params: MythicSkillParam[] = [];
    if (!paramsStr) return params;

    // 支持 ; 和 , 作为分隔符
    const parts = paramsStr.split(/[;,]/);
    for (const part of parts) {
      const trimmed = part.trim();
      if (!trimmed) continue;

      const eqIndex = trimmed.indexOf('=');
      if (eqIndex > 0) {
        params.push({
          key: trimmed.substring(0, eqIndex).trim(),
          value: trimmed.substring(eqIndex + 1).trim(),
        });
      } else {
        // 无值参数
        params.push({ key: trimmed, value: 'true' });
      }
    }

    return params;
  }

  /**
   * 解析条件
   */
  private parseCondition(condStr: string): MythicConditionInfo {
    const match = condStr.match(/~(!?)(\w+)(?:\{([^}]*)\})?/);
    if (!match) {
      return { raw: condStr, type: 'unknown', params: [], negated: false };
    }

    return {
      raw: condStr,
      type: match[2],
      params: match[3] ? this.parseParams(match[3]) : [],
      negated: match[1] === '!',
    };
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

  // ==================== 掉落表功能 ====================

  /**
   * 加载所有掉落表
   */
  async loadDropTables(): Promise<Map<string, MythicDropTableInfo>> {
    const dropTablesPath = this.getDropTablesPath();

    if (!fs.existsSync(dropTablesPath)) {
      return this.dropTableCache;
    }

    try {
      // 递归获取所有 yml 文件
      const files = await this.getAllYmlFiles(dropTablesPath);

      for (const filePath of files) {
        try {
          const content = await fs.promises.readFile(filePath, 'utf8');
          const parsed = yaml.load(content) as Record<string, any>;

          if (!parsed || typeof parsed !== 'object') {
            continue;
          }

          for (const [id, config] of Object.entries(parsed)) {
            if (!config || typeof config !== 'object') {
              continue;
            }

            const dropTable = this.parseDropTable(id, config);
            if (dropTable) {
              this.dropTableCache.set(id, dropTable);
            }
          }
        } catch (fileError) {
          console.error(`解析掉落表文件 ${filePath} 失败:`, fileError);
        }
      }
    } catch (error) {
      console.error('读取掉落表目录失败:', error);
    }

    return this.dropTableCache;
  }

  /**
   * 解析掉落表配置
   */
  private parseDropTable(id: string, config: any): MythicDropTableInfo {
    const dropTable: MythicDropTableInfo = {
      id,
      drops: [],
    };

    // 解析掉落物列表
    if (Array.isArray(config)) {
      dropTable.drops = config.map((drop: any) => this.parseDrop(drop));
    } else if (config.Drops && Array.isArray(config.Drops)) {
      dropTable.drops = config.Drops.map((drop: any) => this.parseDrop(drop));
    }

    // 解析总权重
    if (config.TotalWeight !== undefined) {
      dropTable.totalWeight = this.parseNumber(config.TotalWeight, undefined as any);
    }

    // 解析条件
    if (config.Conditions && Array.isArray(config.Conditions)) {
      dropTable.conditions = config.Conditions.map((c: any) => this.parseCondition(String(c)));
    }

    return dropTable;
  }

  /**
   * 获取指定的掉落表
   */
  async getDropTable(id: string): Promise<MythicDropTableInfo | null> {
    if (this.dropTableCache.size === 0) {
      await this.loadDropTables();
    }

    return this.dropTableCache.get(id) || null;
  }

  /**
   * 获取所有掉落表
   */
  async getAllDropTables(): Promise<MythicDropTableInfo[]> {
    if (this.dropTableCache.size === 0) {
      await this.loadDropTables();
    }

    return Array.from(this.dropTableCache.values());
  }

  // ==================== 技能组功能 ====================

  /**
   * 加载所有技能组
   */
  async loadSkillGroups(): Promise<Map<string, MythicSkillGroupInfo>> {
    const skillsPath = this.getSkillsPath();

    if (!fs.existsSync(skillsPath)) {
      return this.skillGroupCache;
    }

    try {
      const files = await fs.promises.readdir(skillsPath);

      for (const file of files) {
        if (!file.endsWith('.yml') && !file.endsWith('.yaml')) {
          continue;
        }

        try {
          const filePath = path.join(skillsPath, file);
          const content = await fs.promises.readFile(filePath, 'utf8');
          const parsed = yaml.load(content) as Record<string, any>;

          if (!parsed || typeof parsed !== 'object') {
            continue;
          }

          for (const [id, config] of Object.entries(parsed)) {
            if (!config || typeof config !== 'object') {
              continue;
            }

            const skillGroup = this.parseSkillGroup(id, config);
            if (skillGroup) {
              this.skillGroupCache.set(id, skillGroup);
            }
          }
        } catch (fileError) {
          console.error(`解析技能组文件 ${file} 失败:`, fileError);
        }
      }
    } catch (error) {
      console.error('读取技能组目录失败:', error);
    }

    return this.skillGroupCache;
  }

  /**
   * 解析技能组配置
   */
  private parseSkillGroup(id: string, config: any): MythicSkillGroupInfo {
    const skillGroup: MythicSkillGroupInfo = {
      id,
      skills: [],
    };

    // 解析技能列表
    if (config.Skills && Array.isArray(config.Skills)) {
      skillGroup.skills = config.Skills.map((skill: any) => this.parseSkill(skill));
    }

    // 解析冷却时间
    if (config.Cooldown !== undefined) {
      skillGroup.cooldown = this.parseNumber(config.Cooldown, undefined as any);
    }

    // 解析条件
    if (config.Conditions && Array.isArray(config.Conditions)) {
      skillGroup.conditions = config.Conditions.map((c: any) => this.parseCondition(String(c)));
    }

    return skillGroup;
  }

  /**
   * 获取指定的技能组
   */
  async getSkillGroup(id: string): Promise<MythicSkillGroupInfo | null> {
    if (this.skillGroupCache.size === 0) {
      await this.loadSkillGroups();
    }

    return this.skillGroupCache.get(id) || null;
  }

  /**
   * 获取所有技能组
   */
  async getAllSkillGroups(): Promise<MythicSkillGroupInfo[]> {
    if (this.skillGroupCache.size === 0) {
      await this.loadSkillGroups();
    }

    return Array.from(this.skillGroupCache.values());
  }

  // ==================== 模板继承功能 ====================

  /**
   * 构建模板继承关系
   */
  async buildTemplateInheritance(): Promise<Map<string, MythicTemplateInfo>> {
    const mobs = await this.getAllMobs();

    // 首先收集所有怪物的 Template 信息
    const templateMap = new Map<string, { parent?: string; config: any }>();

    for (const mob of mobs) {
      try {
        const filePath = path.join(this.mobsPath, mob.fileName);
        const content = await fs.promises.readFile(filePath, 'utf8');
        const parsed = yaml.load(content) as Record<string, any>;

        if (parsed && parsed[mob.id]) {
          const config = parsed[mob.id];
          templateMap.set(mob.id, {
            parent: config.Template ? String(config.Template) : undefined,
            config,
          });
        }
      } catch (error) {
        // 忽略读取错误
      }
    }

    // 构建继承关系
    for (const [id, info] of templateMap) {
      const templateInfo: MythicTemplateInfo = {
        id,
        parent: info.parent,
        children: [],
        depth: 0,
      };

      // 计算继承深度
      let current = info.parent;
      let depth = 0;
      while (current && templateMap.has(current) && depth < 10) {
        depth++;
        current = templateMap.get(current)?.parent;
      }
      templateInfo.depth = depth;

      this.templateCache.set(id, templateInfo);
    }

    // 填充子模板列表
    for (const [id, info] of this.templateCache) {
      if (info.parent && this.templateCache.has(info.parent)) {
        this.templateCache.get(info.parent)!.children.push(id);
      }
    }

    return this.templateCache;
  }

  /**
   * 获取怪物的模板继承信息
   */
  async getTemplateInfo(mobId: string): Promise<MythicTemplateInfo | null> {
    if (this.templateCache.size === 0) {
      await this.buildTemplateInheritance();
    }

    return this.templateCache.get(mobId) || null;
  }

  /**
   * 获取完整的继承链
   */
  async getInheritanceChain(mobId: string): Promise<string[]> {
    if (this.templateCache.size === 0) {
      await this.buildTemplateInheritance();
    }

    const chain: string[] = [mobId];
    let current = this.templateCache.get(mobId)?.parent;

    while (current && this.templateCache.has(current)) {
      chain.push(current);
      current = this.templateCache.get(current)?.parent;
    }

    return chain;
  }

  // ==================== 配置编辑功能 ====================

  /**
   * 获取怪物的原始 YAML 配置
   */
  async getMobRawYaml(mobId: string): Promise<string | null> {
    const mob = await this.getMobById(mobId);
    if (!mob) return null;

    try {
      const filePath = path.join(this.mobsPath, mob.fileName);
      const content = await fs.promises.readFile(filePath, 'utf8');
      const parsed = yaml.load(content) as Record<string, any>;

      if (parsed && parsed[mobId]) {
        // 只返回这个怪物的配置
        return yaml.dump({ [mobId]: parsed[mobId] }, { indent: 2, lineWidth: -1 });
      }
    } catch (error) {
      console.error(`读取怪物配置失败 ${mobId}:`, error);
    }

    return null;
  }

  /**
   * 保存怪物配置
   */
  async saveMobConfig(mobId: string, newConfig: Record<string, any>): Promise<boolean> {
    const mob = await this.getMobById(mobId);
    if (!mob) return false;

    try {
      const filePath = path.join(this.mobsPath, mob.fileName);
      const content = await fs.promises.readFile(filePath, 'utf8');
      const parsed = yaml.load(content) as Record<string, any>;

      if (!parsed) return false;

      // 更新配置
      parsed[mobId] = newConfig;

      // 写回文件
      const newContent = yaml.dump(parsed, { indent: 2, lineWidth: -1 });
      await fs.promises.writeFile(filePath, newContent, 'utf8');

      // 清除缓存
      this.clearCache();

      return true;
    } catch (error) {
      console.error(`保存怪物配置失败 ${mobId}:`, error);
      return false;
    }
  }

  /**
   * 验证 YAML 配置
   */
  validateYamlConfig(yamlContent: string): { valid: boolean; error?: string; parsed?: any } {
    try {
      const parsed = yaml.load(yamlContent);
      return { valid: true, parsed };
    } catch (error: any) {
      return { valid: false, error: error.message };
    }
  }

  // ==================== 增强的详情获取 ====================

  /**
   * 获取怪物详细信息（增强版，包含掉落表展开和模板信息）
   */
  async getMobDetailByIdEnhanced(id: string): Promise<MythicMobDetailInfo | null> {
    const detail = await this.getMobDetailById(id);
    if (!detail) return null;

    // 展开掉落表引用
    if (detail.dropsTable) {
      const dropTable = await this.getDropTable(detail.dropsTable);
      if (dropTable) {
        detail.expandedDropsTable = dropTable;
      }
    }

    // 获取模板继承信息
    const templateInfo = await this.getTemplateInfo(id);
    if (templateInfo) {
      detail.template = templateInfo.parent;
      detail.templateInfo = templateInfo;
    }

    // 解析技能组引用
    const skillGroups: MythicSkillGroupInfo[] = [];
    for (const skill of detail.parsedSkills) {
      if (skill.mechanic?.toLowerCase() === 'skill' && skill.params) {
        const skillParam = skill.params.find(p => p.key.toLowerCase() === 's' || p.key.toLowerCase() === 'skill');
        if (skillParam) {
          const skillGroup = await this.getSkillGroup(skillParam.value);
          if (skillGroup) {
            skillGroups.push(skillGroup);
          }
        }
      }
    }
    if (skillGroups.length > 0) {
      detail.skillGroups = skillGroups;
    }

    return detail;
  }

  // ==================== 物品功能 ====================

  /**
   * 加载所有物品
   */
  async loadItems(): Promise<Map<string, MythicItemInfo>> {
    const itemsPath = this.getItemsPath();

    if (!fs.existsSync(itemsPath)) {
      return this.itemCache;
    }

    try {
      // 递归获取所有 yml 文件
      const files = await this.getAllYmlFiles(itemsPath);

      for (const filePath of files) {
        try {
          const content = await fs.promises.readFile(filePath, 'utf8');
          const parsed = yaml.load(content) as Record<string, any>;

          if (!parsed || typeof parsed !== 'object') {
            continue;
          }

          // 获取相对文件名用于显示
          const fileName = path.relative(itemsPath, filePath);

          for (const [id, config] of Object.entries(parsed)) {
            if (!config || typeof config !== 'object') {
              continue;
            }

            const item = this.parseItemConfig(id, config, fileName);
            if (item) {
              this.itemCache.set(id, item);
            }
          }
        } catch (fileError) {
          console.error(`解析物品文件 ${filePath} 失败:`, fileError);
        }
      }
    } catch (error) {
      console.error('读取物品目录失败:', error);
    }

    return this.itemCache;
  }

  /**
   * 解析物品配置
   */
  private parseItemConfig(id: string, config: any, fileName: string): MythicItemInfo | null {
    try {
      // 清理显示名称中的颜色代码
      let displayName = config.Display || config.DisplayName || id;
      displayName = this.stripColorCodes(displayName);

      const item: MythicItemInfo = {
        id,
        displayName,
        material: config.Id || config.Material || 'STONE',
        fileName,
      };

      // 数量
      if (config.Amount !== undefined) {
        item.amount = this.parseNumber(config.Amount, 1);
      }

      // 自定义模型数据
      if (config.Model !== undefined || config.CustomModelData !== undefined) {
        item.customModelData = this.parseNumber(config.Model || config.CustomModelData, undefined as any);
      }

      // Lore (描述)
      if (config.Lore && Array.isArray(config.Lore)) {
        item.lore = config.Lore.map((line: any) => this.stripColorCodes(String(line)));
      }

      // 附魔
      if (config.Enchantments && Array.isArray(config.Enchantments)) {
        item.enchantments = this.parseEnchantments(config.Enchantments);
      }

      // 属性修饰符
      if (config.Attributes && Array.isArray(config.Attributes)) {
        item.attributes = this.parseAttributes(config.Attributes);
      }

      // 不可破坏
      if (config.Unbreakable !== undefined) {
        item.unbreakable = Boolean(config.Unbreakable);
      }

      // 隐藏标志
      if (config.HideFlags && Array.isArray(config.HideFlags)) {
        item.hideFlags = config.HideFlags.map((f: any) => String(f));
      }

      // 皮革颜色
      if (config.Color) {
        item.color = String(config.Color);
      }

      // 药水效果
      if (config.PotionEffects && Array.isArray(config.PotionEffects)) {
        item.potionEffects = config.PotionEffects.map((e: any) => String(e));
      }

      // 头颅材质
      if (config.SkullTexture || config.Texture) {
        item.skullTexture = String(config.SkullTexture || config.Texture);
      }

      // NBT 数据
      if (config.NBT) {
        item.nbt = config.NBT;
      }

      // Options
      if (config.Options && typeof config.Options === 'object') {
        item.options = config.Options;
      }

      return item;
    } catch (error) {
      console.error(`解析物品配置 ${id} 失败:`, error);
      return null;
    }
  }

  /**
   * 解析附魔列表
   */
  private parseEnchantments(enchantments: any[]): MythicEnchantmentInfo[] {
    const result: MythicEnchantmentInfo[] = [];

    for (const ench of enchantments) {
      const str = String(ench);
      // 格式: ENCHANTMENT:LEVEL 或 ENCHANTMENT LEVEL
      const match = str.match(/^([A-Z_]+)[:\s](\d+)$/i);
      if (match) {
        result.push({
          enchantment: match[1].toUpperCase(),
          level: parseInt(match[2], 10),
        });
      } else {
        // 只有附魔名，等级默认为1
        result.push({
          enchantment: str.toUpperCase(),
          level: 1,
        });
      }
    }

    return result;
  }

  /**
   * 解析属性修饰符列表
   */
  private parseAttributes(attributes: any[]): MythicAttributeInfo[] {
    const result: MythicAttributeInfo[] = [];

    for (const attr of attributes) {
      if (typeof attr === 'string') {
        // 格式: ATTRIBUTE AMOUNT [OPERATION] [SLOT]
        const parts = attr.split(/\s+/);
        if (parts.length >= 2) {
          const attrInfo: MythicAttributeInfo = {
            attribute: parts[0].toUpperCase(),
            amount: parseFloat(parts[1]) || 0,
          };
          if (parts.length >= 3) {
            attrInfo.operation = parts[2];
          }
          if (parts.length >= 4) {
            attrInfo.slot = parts[3];
          }
          result.push(attrInfo);
        }
      } else if (typeof attr === 'object') {
        // 对象格式
        result.push({
          attribute: String(attr.Attribute || attr.Type || '').toUpperCase(),
          amount: this.parseNumber(attr.Amount || attr.Value, 0),
          operation: attr.Operation,
          slot: attr.Slot,
        });
      }
    }

    return result;
  }

  /**
   * 获取所有物品
   */
  async getAllItems(): Promise<MythicItemInfo[]> {
    if (this.itemCache.size === 0) {
      await this.loadItems();
    }

    return Array.from(this.itemCache.values());
  }

  /**
   * 获取指定物品
   */
  async getItem(id: string): Promise<MythicItemInfo | null> {
    if (this.itemCache.size === 0) {
      await this.loadItems();
    }

    return this.itemCache.get(id) || null;
  }

  /**
   * 获取物品详细信息
   */
  async getItemDetail(id: string): Promise<MythicItemDetailInfo | null> {
    const item = await this.getItem(id);
    if (!item) return null;

    try {
      const filePath = path.join(this.getItemsPath(), item.fileName);
      const content = await fs.promises.readFile(filePath, 'utf8');
      const parsed = yaml.load(content) as Record<string, any>;

      if (!parsed || !parsed[id]) {
        return null;
      }

      return {
        ...item,
        rawConfig: parsed[id],
      };
    } catch (error) {
      console.error(`获取物品详情 ${id} 失败:`, error);
      return null;
    }
  }

  /**
   * 搜索物品
   */
  async searchItems(keyword: string): Promise<MythicItemInfo[]> {
    const items = await this.getAllItems();
    const lowerKeyword = keyword.toLowerCase();
    return items.filter(item =>
      item.id.toLowerCase().includes(lowerKeyword) ||
      item.displayName.toLowerCase().includes(lowerKeyword) ||
      item.material.toLowerCase().includes(lowerKeyword)
    );
  }

  /**
   * 获取物品材质统计
   */
  async getItemMaterialStats(): Promise<Record<string, number>> {
    const items = await this.getAllItems();
    const stats: Record<string, number> = {};

    for (const item of items) {
      const material = item.material.toUpperCase();
      stats[material] = (stats[material] || 0) + 1;
    }

    return stats;
  }

  // ==================== 物品配置编辑功能 ====================

  /**
   * 获取物品的原始 YAML 配置
   */
  async getItemRawYaml(itemId: string): Promise<string | null> {
    const item = await this.getItem(itemId);
    if (!item) return null;

    try {
      const filePath = path.join(this.getItemsPath(), item.fileName);
      const content = await fs.promises.readFile(filePath, 'utf8');
      const parsed = yaml.load(content) as Record<string, any>;

      if (parsed && parsed[itemId]) {
        // 只返回这个物品的配置
        return yaml.dump({ [itemId]: parsed[itemId] }, { indent: 2, lineWidth: -1 });
      }
    } catch (error) {
      console.error(`读取物品配置失败 ${itemId}:`, error);
    }

    return null;
  }

  /**
   * 保存物品配置
   */
  async saveItemConfig(itemId: string, newConfig: Record<string, any>): Promise<boolean> {
    const item = await this.getItem(itemId);
    if (!item) return false;

    try {
      const filePath = path.join(this.getItemsPath(), item.fileName);
      const content = await fs.promises.readFile(filePath, 'utf8');
      const parsed = yaml.load(content) as Record<string, any>;

      if (!parsed) return false;

      // 更新配置
      parsed[itemId] = newConfig;

      // 写回文件
      const newContent = yaml.dump(parsed, { indent: 2, lineWidth: -1 });
      await fs.promises.writeFile(filePath, newContent, 'utf8');

      // 清除缓存
      this.clearCache();

      return true;
    } catch (error) {
      console.error(`保存物品配置失败 ${itemId}:`, error);
      return false;
    }
  }
}

export default new MythicMobsService();
