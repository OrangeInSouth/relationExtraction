__author__ = "yongjie liu"

import traceback
import os
import sys
import re

root_path = os.path.abspath(os.path.dirname(os.path.dirname(__file__)))


# 将list的最大$小值输出
def outputListMax(temp_list):
    if len(temp_list) < 1:
        return None, None
    max = temp_list[0]
    min = temp_list[0]
    for x in temp_list:
        if x > max:
            max = x
        if x < min:
            min = x

    return min, max


# 如果b的字符串在a中，调整a
def repairList(list_a, list_b):
    length1 = 0
    while length1 < len(list_a):
        now_temp = list_a[length1]
        if now_temp in list_b:
            # 去除所有比x小的
            length2 = 0
            while length2 < len(list_a):
                if list_a[length2] <= now_temp:
                    list_a.pop(length2)
                    if length2 <= length1:
                        length1 -= 1
                    continue
                length2 += 1
        length1 += 1


def fact_triple_extract(sentence, segmentor, postagger, recognizer, parser):
    total = 0  # 抽取出三元组数目
    type1 = 0  # 主谓宾数目
    type2 = 0  # 定语后置数目
    type3 = 0  # 介宾主谓动补数目
    type4 = 0  # ATT数目
    """
    对于给定的句子进行事实三元组抽取
    Args:
        sentence: 要处理的语句
    """
    # print sentence
    words = segmentor.segment(sentence)  # 分词
    print("word is ")

    wordsStr = "\t".join(words)
    print(wordsStr)

    postags = postagger.postag(words)  # 命名实体识别
    netags = recognizer.recognize(words, postags)
    print("netags is ")

    netagsStr = "\t".join(netags)
    print(netagsStr)

    arcs = parser.parse(words, postags)  # 依存句法分析：arc.head 表示依存弧的父节点词的索引，arc.relation 表示依存弧的关系。
    print("arc.head:arc.relation is ")

    arcsStr = "\t".join("%d:%s" % (arc.head, arc.relation) for arc in arcs)
    print(arcsStr)

    print("postags is")  # 词性标注

    postagsStr = "\t".join(postags)
    print(postagsStr)

    NE_list = set()

    train_pattern = ['O' for x in netags]
    output_sentence = []
    triple_list = []
    log_info = ""
    for i in range(len(netags)):
        # netags is vectorOfString
        # S是一个名词
        # B表示开始
        if netags[i][0] == 'S' or netags[i][0] == 'B':
            j = i
            if netags[j][0] == 'B':
                while j < len(netags) and netags[j][0] != 'E':
                    j += 1
                if j >= len(netags):
                    continue
                e = ''.join(words[i:j + 1])
                NE_list.add(e)
            else:
                e = words[j]
                NE_list.add(e)

    # 至此名词全部收集在NE_list

    child_dict_list = build_parse_child_dict(words, arcs)

    print(child_dict_list)
    sign = 0
    for index in range(len(postags)):
        output_sentence.append(words[index])
        # 抽取以谓词为中心的事实三元组
        if postags[index] == 'v':
            child_dict = child_dict_list[index]
            # 主谓宾
            if 'SBV' in child_dict and 'VOB' in child_dict:
                index_h = []
                index_t = []
                index_r = []
                e1 = complete_e(words, postags, child_dict_list, child_dict['SBV'][0], index_h)
                r = words[index]
                index_r.append(index)
                e2 = complete_e(words, postags, child_dict_list, child_dict['VOB'][0], index_t)

                # 该句若注释，则所有主谓动宾短语都会被抽取
                if is_good(e1, NE_list, sentence, segmentor, postagger) and is_good(e2, NE_list, sentence, segmentor,
                postagger):

                    # -----------若去除if语句注释，该区域代码需放在if语句之内------------------
                    log_info += "主语谓语宾语关系\t(%s, %s, %s)\t%s\t%s\n" % (e1, r, e2, sentence, 0)

                    min1, max1 = outputListMax(index_h)
                    min2, max2 = outputListMax(index_r)
                    min3, max3 = outputListMax(index_t)
                    if min1 != None and min2 != None and min3 != None and max1 != None and max2 != None and max3 != None:
                        triple_list.append(min1)
                        triple_list.append(max1)
                        triple_list.append(min2)
                        triple_list.append(max2)
                        triple_list.append(min3)
                        triple_list.append(max3)

                    for x in index_h:
                        train_pattern.pop(x)
                        train_pattern.insert(x, 'H')
                    for x in index_r:
                        train_pattern.pop(x)
                        train_pattern.insert(x, 'R')
                    for x in index_t:
                        train_pattern.pop(x)
                        train_pattern.insert(x, 'T')

                    sign = 1
                    type1 += 1
                    total += 1
            # -----------若去除if语句注释，该区域代码需放在if语句之内------------------
            # 定语后置，动宾关系
            if arcs[index].relation == 'ATT':
                index_h = []
                index_t = []
                if 'VOB' in child_dict:
                    e1 = complete_e(words, postags, child_dict_list, arcs[index].head - 1, index_h)
                    r = words[index]
                    index_r = [index]
                    e2 = complete_e(words, postags, child_dict_list, child_dict['VOB'][0], index_t)
                    temp_string = r + e2
                    if temp_string == e1[:len(temp_string)]:
                        e1 = e1[len(temp_string):]
                        repairList(index_h, index_r + index_t)

                    # if temp_string not in e1 and (e1 in NE_list or e2 in NE_list):
                    if temp_string not in e1 and is_good(e1, NE_list, sentence, segmentor, postagger) and is_good(e2,
                                                                                                                  NE_list,
                                                                                                                  sentence,
                                                                                                                  segmentor,
                                                                                                                  postagger):
                        log_info += "定语后置动宾关系\t(%s, %s, %s)\t%s\t%s\n" % (e1, r, e2, sentence, 0)

                        min1, max1 = outputListMax(index_h)
                        min2, max2 = outputListMax(index_r)
                        min3, max3 = outputListMax(index_t)
                        if min1 != None and min2 != None and min3 != None and max1 != None and max2 != None and max3 != None:
                            triple_list.append(min1)
                            triple_list.append(max1)
                            triple_list.append(min2)
                            triple_list.append(max2)
                            triple_list.append(min3)
                            triple_list.append(max3)

                        for x in index_h:
                            train_pattern.pop(x)
                            train_pattern.insert(x, 'H')
                        for x in index_r:
                            train_pattern.pop(x)
                            train_pattern.insert(x, 'R')
                        for x in index_t:
                            train_pattern.pop(x)
                            train_pattern.insert(x, 'T')
                        sign = 1
                        type2 += 1
                        total += 1
            # 含有介宾关系的主谓动补关系
            if 'SBV' in child_dict and 'CMP' in child_dict:
                index_h = []
                index_t = []
                e1 = complete_e(words, postags, child_dict_list, child_dict['SBV'][0], index_h)
                cmp_index = child_dict['CMP'][0]
                r = words[index] + words[cmp_index]
                index_r = [index, cmp_index]
                if 'POB' in child_dict_list[cmp_index]:
                    e2 = complete_e(words, postags, child_dict_list, child_dict_list[cmp_index]['POB'][0], index_t)
                    # if e1 in NE_list or e2 in NE_list:
                    if is_good(e1, NE_list, sentence, segmentor, postagger) and is_good(e2, NE_list, sentence,
                                                                                        segmentor, postagger):
                        log_info += "介宾关系主谓动补\t(%s, %s, %s)\t%s\t%s\n" % (e1, r, e2, sentence, 0)

                        min1, max1 = outputListMax(index_h)
                        min2, max2 = outputListMax(index_r)
                        min3, max3 = outputListMax(index_t)
                        if min1 != None and min2 != None and min3 != None and max1 != None and max2 != None and max3 != None:
                            triple_list.append(min1)
                            triple_list.append(max1)
                            triple_list.append(min2)
                            triple_list.append(max2)
                            triple_list.append(min3)
                            triple_list.append(max3)
                        for x in index_h:
                            train_pattern.pop(x)
                            train_pattern.insert(x, 'H')
                        for x in index_r:
                            train_pattern.pop(x)
                            train_pattern.insert(x, 'R')
                        for x in index_t:
                            train_pattern.pop(x)
                            train_pattern.insert(x, 'T')
                        sign = 1
                        type3 += 1
                        total += 1

        # 尝试抽取命名实体有关的三元组
        if netags[index][0] == 'S' or netags[index][0] == 'B':
            ni = index
            index_t = []
            index_r = []
            if netags[ni][0] == 'B':
                while ni < len(netags) and netags[ni][0] != 'E':
                    ni += 1
                e1 = ''.join(words[index:ni + 1])
                index_h = [x for x in range(index, ni + 1)]

            else:
                e1 = words[ni]
                index_h = [ni]
            if ni >= len(netags):
                continue

            # ni是最后一个命名实体的下标
            if arcs[ni].relation == 'ATT' and postags[arcs[ni].head - 1] == 'n' and netags[arcs[ni].head - 1] == 'O':
                r = complete_e(words, postags, child_dict_list, arcs[ni].head - 1, index_r)
                if e1 in r:
                    r = r[(r.index(e1) + len(e1)):]
                    repairList(index_r, index_h)

                if arcs[arcs[ni].head - 1].relation == 'ATT' and netags[arcs[arcs[ni].head - 1].head - 1] != 'O':
                    e2 = complete_e(words, postags, child_dict_list, arcs[arcs[ni].head - 1].head - 1, index_t)
                    mi = arcs[arcs[ni].head - 1].head - 1
                    li = mi
                    if netags[mi][0] == 'B':
                        while netags[mi][0] != 'E':
                            mi += 1
                        e = ''.join(words[li + 1:mi + 1])
                        index_t.extend([x for x in range(li + 1, mi + 1)])
                        e2 += e
                    if r in e2:
                        e2 = e2[(e2.index(r) + len(r)):]
                        repairList(index_t, index_r)

                    if is_good(e1, NE_list, sentence, segmentor, postagger) and is_good(e2, NE_list, sentence,
                                                                                        segmentor, postagger):
                        log_info += "(人名/地名/机构,职务/关系,命名实体)\t(%s,%s,%s)\t%s\t%s\n" % (e1, r, e2, sentence, 1)

                        min1, max1 = outputListMax(index_h)
                        min2, max2 = outputListMax(index_r)
                        min3, max3 = outputListMax(index_t)
                        if min1 != None and min2 != None and min3 != None and max1 != None and max2 != None and max3 != None:
                            triple_list.append(min1)
                            triple_list.append(max1)
                            triple_list.append(min2)
                            triple_list.append(max2)
                            triple_list.append(min3)
                            triple_list.append(max3)
                        for x in index_h:
                            train_pattern.pop(x)
                            train_pattern.insert(x, 'H')
                        for x in index_r:
                            train_pattern.pop(x)
                            train_pattern.insert(x, 'R')
                        for x in index_t:
                            train_pattern.pop(x)
                            train_pattern.insert(x, 'T')

                        sign = 1
                        type4 += 1
                        total += 1
    # print(output_sentence)
    # print(train_pattern)
    # if sign == 1:
    #     observation.write(output_sentence)
    #     observation.write('\n')
    #     temp = ""
    #     for x in train_pattern:
    #         temp += (str(x) + " ")
    #     observation.write(temp)
    #     observation.write('\n')
    #     temp = ""
    #     for x in triple_list:
    #         temp += (str(x) + " ")
    #     observation.write(temp)
    #     observation.write('\n\n')
    #     observation.flush()
    return log_info, output_sentence, train_pattern


def build_parse_child_dict(words, arcs):
    """
    为句子中的每个词语维护一个保存句法依存儿子节点的字典
    Args:
        words: 分词列表
        postags: 词性列表
        arcs: 句法依存列表
    """
    child_dict_list = []
    for index in range(len(words)):
        child_dict = dict()
        for arc_index in range(len(arcs)):
            if arcs[arc_index].head == index + 1:
                if arcs[arc_index].relation in child_dict:
                    child_dict[arcs[arc_index].relation].append(arc_index)
                else:
                    child_dict[arcs[arc_index].relation] = []
                    child_dict[arcs[arc_index].relation].append(arc_index)
        # if child_dict.has_key('SBV'):
        #    print words[index],child_dict['SBV']
        child_dict_list.append(child_dict)
    return child_dict_list


def complete_e(words, postags, child_dict_list, word_index, my_list):
    """
    完善识别的部分实体
    """
    # 美国，总统，奥巴马 word_index对应总统
    # 台湾 属于 中国 word_index对应台湾与中国
    my_list.append(word_index)
    child_dict = child_dict_list[word_index]
    prefix = ''
    if 'ATT' in child_dict:
        for i in range(len(child_dict['ATT'])):
            prefix += complete_e(words, postags, child_dict_list, child_dict['ATT'][i], my_list)

            # todo
            # 中国驻俄罗斯大使，想办法改一改
    postfix = ''
    if postags[word_index] == 'v':
        if 'VOB' in child_dict:
            postfix += complete_e(words, postags, child_dict_list, child_dict['VOB'][0], my_list)
        if 'SBV' in child_dict:
            prefix = complete_e(words, postags, child_dict_list, child_dict['SBV'][0], my_list) + prefix

    return prefix + words[word_index] + postfix


def include(a, b):
    if a in b or b in a:
        return True
    else:
        return False


# TODO
# 该方法需要改进
def calculateSimiliar(e, NE_list):
    for x in NE_list:
        if include(x, e):
            return True


def is_good(e, NE_list, sentence, segmentor, postagger):
    """
    判断e是否为命名实体
    """
    if e not in sentence:
        return False

    words_e = segmentor.segment(e)
    postags_e = postagger.postag(words_e)
    if calculateSimiliar(e, NE_list):
        return True


    else:
        NE_count = 0
        for i in range(len(words_e)):
            if words_e[i] in NE_list:
                NE_count += 1
            if postags_e[i] == 'v':
                return False
        if NE_count >= len(words_e) - NE_count:
            return True
    return False


def main(sentences, segmentor, postagger, recognizer, parser):
    result = []
    move_i = 0
    while move_i < len(sentences):
        start_i = move_i
        while move_i < len(sentences) and sentences[move_i] != '，' and sentences[move_i] != ',' and sentences[
            move_i] != '。' and sentences[move_i] != '.':
            move_i += 1
        sentence = sentences[start_i:move_i]
        log_info, output_sentence, train_pattern = fact_triple_extract(sentence,
                                                                       segmentor,
                                                                       postagger,
                                                                       recognizer,
                                                                       parser)

        if move_i < len(sentences):
            if sentences[move_i] == '，':
                output_sentence.append('，')
                train_pattern.append('O')
            if sentences[move_i] == ',':
                output_sentence.append(',')
                train_pattern.append('O')
            if sentences[move_i] == '。':
                output_sentence.append('。')
                train_pattern.append('O')
            if sentences[move_i] == '.':
                output_sentence.append('.')
                train_pattern.append('O')

        print(log_info)
        try:
            assert len(output_sentence) == len(train_pattern)
        except:
            traceback.print_exc()
            log_info = ""
            output_sentence = []
            train_pattern = []

        # 把output_sentence信息和train_pattern转化为前端信息
        if len(train_pattern) <= 1:
            pass
        else:
            temp_result = ""
            for i, n in enumerate(train_pattern):
                if i == 0:
                    temp_result += output_sentence[i]
                else:
                    if train_pattern[i] != train_pattern[i - 1]:
                        if train_pattern[i - 1] == 'O':
                            result.append(['0', temp_result])

                        if train_pattern[i - 1] == 'H':
                            result.append(['1', temp_result])

                        if train_pattern[i - 1] == 'R':
                            result.append(['2', temp_result])

                        if train_pattern[i - 1] == 'T':
                            result.append(['3', temp_result])
                        temp_result = ""

                    temp_result += output_sentence[i]
            if train_pattern[i] == 'O':
                result.append(['0', temp_result])

            if train_pattern[i] == 'H':
                result.append(['1', temp_result])
            if train_pattern[i] == 'R':
                result.append(['2', temp_result])
            if train_pattern[i] == 'T':
                result.append(['3', temp_result])
        move_i += 1
        # sen_list = sentences.split('\n')
        # post_sen_list = []
        # for x in sen_list:
        #     if x != sen_list[-1]:
        #         x = x + '\n'
        #     x = x.replace('，', '\n').replace(',', '\n')
        #     x = x.replace('。', '\n').replace('.', '\n')
        #     x = x.replace('\t', '')
        #     x = x.replace('[', '')
        #     x = x.replace(']', '')
        #     x = x.replace(';', '')
        #     x = x.replace('；', '')
        #     x = x.replace('　', '').replace(' ', '')
        #     x = re.sub(r'\n+', "\n", x)
        #     if x != '\n':
        #         temp_list = sen_list = x.split('\n')
        #         for y in temp_list:
        #             if y != '':
        #                 post_sen_list.append(y)

        # print("抽取出了" + str(a) + "个三元组")
        # print("主谓宾" + str(t1))
        # print("定语后置" + str(t2))
        # print("介宾主谓动补" + str(t3))
        # print("ATT" + str(t4))
    return result


if __name__ == "__main__":
    main('國家主席胡锦涛')
